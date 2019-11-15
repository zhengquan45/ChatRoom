package org.zhq.impl.stealing;

import org.zhq.core.IoProvider;
import org.zhq.core.IoTask;
import org.zhq.utils.CloseUtil;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class StealingSelectorThread extends Thread {

    private static final int VALID_OPS = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    private final Selector selector;
    private volatile boolean running = true;
    private final LinkedBlockingQueue<IoTask> readyTaskQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<IoTask> registerTaskQueue = new LinkedBlockingQueue<>();
    private final List<IoTask> onceReadyTaskCache = new ArrayList<>(200);

    private final AtomicLong saturatingCapacity = new AtomicLong();

    private volatile StealingService stealingService;

    protected StealingSelectorThread(Selector selector) {
        this.selector = selector;
    }

    public void setStealingService(StealingService stealingService) {
        this.stealingService = stealingService;
    }

    public boolean register(SocketChannel channel, int ops, IoProvider.HandleProviderTask callback) {
        if (channel.isOpen()) {
            IoTask ioTask = new IoTask(channel, callback, ops);
            registerTaskQueue.offer(ioTask);
            return true;
        }
        return false;
    }

    public void unregister(SocketChannel channel) {
        SelectionKey selectionKey = channel.keyFor(selector);
        if (selectionKey != null && selectionKey.attachment() != null) {
            selectionKey.attach(null);
            IoTask ioTask = new IoTask(channel, null, 0);
            registerTaskQueue.offer(ioTask);
        }
    }

    public void exit() {
        running = false;
        CloseUtil.close(selector);
        interrupt();
    }

    private void consumeRegisterTodoTasks(final LinkedBlockingQueue<IoTask> registerTaskQueue) {
        final Selector selector = this.selector;

        IoTask registerTask = registerTaskQueue.poll();
        while (registerTask != null) {
            try {
                final SocketChannel channel = registerTask.channel;
                int ops = registerTask.ops;
                if (ops == 0) {
                    //unregister
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                    }
                } else if ((ops & ~VALID_OPS) == 0) {
                    //register
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        // Already registered other ops. just use | add interest event
                        key.interestOps(key.interestOps() | ops);
                    } else {
                        // Not registered. call register method add interest event
                        key = channel.register(selector, ops, new KeyAttachment());
                    }
                    Object attachment = key.attachment();
                    if (attachment != null) {
                        ((KeyAttachment) attachment).attach(ops, registerTask);
                    } else {
                        key.cancel();
                    }
                }
            } catch (ClosedChannelException
                    | CancelledKeyException
                    | ClosedSelectorException ignored) {
            } finally {
                registerTask = registerTaskQueue.poll();
            }
        }
    }

    private void joinTaskQueue(final LinkedBlockingQueue<IoTask> readyTaskQueue, final List<IoTask> onceReadyTaskCache) {
        readyTaskQueue.addAll(onceReadyTaskCache);
    }

    private void consumeTodoTasks(final LinkedBlockingQueue<IoTask> readyTaskQueue, LinkedBlockingQueue<IoTask> registerTaskQueue) {
        IoTask task = readyTaskQueue.poll();
        final AtomicLong saturatingCapacity = this.saturatingCapacity;
        while (task != null) {
            saturatingCapacity.incrementAndGet();
            if (processTask(task)) {
                registerTaskQueue.offer(task);
            }
            task = readyTaskQueue.poll();
        }

        final StealingService stealingService = this.stealingService;
        if(stealingService!=null){
            task = stealingService.steal(readyTaskQueue);
            while(task!=null){
                saturatingCapacity.incrementAndGet();
                if (processTask(task)) {
                    registerTaskQueue.offer(task);
                }
                task = stealingService.steal(readyTaskQueue);
            }
        }

    }

    @Override
    public final void run() {
        super.run();

        final Selector selector = this.selector;
        final LinkedBlockingQueue<IoTask> registerTaskQueue = this.registerTaskQueue;
        final LinkedBlockingQueue<IoTask> readyTaskQueue = this.readyTaskQueue;
        final List<IoTask> onceReadyTaskCache = this.onceReadyTaskCache;

        try {
            while (running) {
                //iterate registerTaskQueue to register interest event
                consumeRegisterTodoTasks(registerTaskQueue);

                if ((selector.selectNow()) == 0) {
                    Thread.yield();
                    continue;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    Object attachment = selectionKey.attachment();
                    if (selectionKey.isValid() && attachment instanceof KeyAttachment) {
                        KeyAttachment keyAttachment = (KeyAttachment) attachment;
                        try {
                            //ready event
                            final int readyOps = selectionKey.readyOps();
                            //interest event. ps:interest event maybe not ready
                            int interestOps = selectionKey.interestOps();
                            if ((readyOps & SelectionKey.OP_READ) != 0) {
                                onceReadyTaskCache.add(keyAttachment.taskForReadable);
                                interestOps = interestOps & ~SelectionKey.OP_READ;
                            }

                            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                                onceReadyTaskCache.add(keyAttachment.taskForWritable);
                                interestOps = interestOps & ~SelectionKey.OP_WRITE;
                            }

                            selectionKey.interestOps(interestOps);
                        } catch (CancelledKeyException ignored) {
                            onceReadyTaskCache.remove(keyAttachment.taskForReadable);
                            onceReadyTaskCache.remove(keyAttachment.taskForWritable);
                        }
                    }
                    iterator.remove();
                }

                if (!onceReadyTaskCache.isEmpty()) {
                    joinTaskQueue(readyTaskQueue, onceReadyTaskCache);
                    onceReadyTaskCache.clear();
                }
                consumeTodoTasks(readyTaskQueue, registerTaskQueue);
            }
        } catch (ClosedSelectorException ignored) {
        } catch (IOException e) {
            CloseUtil.close(selector);
        } finally {
            readyTaskQueue.clear();
            registerTaskQueue.clear();
            onceReadyTaskCache.clear();
        }
    }

    protected abstract boolean processTask(IoTask task);

    public LinkedBlockingQueue<IoTask> getReadyTaskQueue() {
        return readyTaskQueue;
    }

    public long getSaturatingCapacity() {
        if (selector.isOpen()) {
            return saturatingCapacity.get();
        } else {
            return -1;
        }
    }

    static class KeyAttachment {
        IoTask taskForReadable;
        IoTask taskForWritable;

        void attach(int ops, IoTask task) {
            if (SelectionKey.OP_READ == ops) {
                taskForReadable = task;
            } else {
                taskForWritable = task;
            }
        }
    }
}
