package impl;

import core.IoProvider;
import core.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);
    private final Selector readSelector;
    private final Selector writeSelector;
    private final Map<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final Map<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();
    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;

    public IoSelectorProvider() throws IOException {
        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
        this.inputHandlePool = Executors.newFixedThreadPool(4, new NamedThreadFactory("IoSelectorProvider-Input-"));
        this.outputHandlePool = Executors.newFixedThreadPool(4, new NamedThreadFactory("IoSelectorProvider-Output-"));
        startRead();
        startWrite();
    }

    private void startWrite() {
        Thread writeThread = new SelectorThread("Write Thread Listener", closed, inRegOutput, writeSelector, outputCallbackMap, outputHandlePool, SelectionKey.OP_WRITE);
        writeThread.start();
    }

    private void startRead() {
        Thread readThread = new SelectorThread("Read Thread Listener", closed, inRegInput, readSelector, inputCallbackMap, inputHandlePool, SelectionKey.OP_READ);
        readThread.start();
    }

    public boolean registerInput(SocketChannel channel, HandleProviderTask callback) {
        return registerSelector(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callback) != null;
    }

    public boolean registerOutput(SocketChannel channel, HandleProviderTask callback) {
        return registerSelector(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callback) != null;
    }

    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap, inRegInput);
    }


    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap, inRegOutput);
    }

    private static void waitSelection(final AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> map, AtomicBoolean locker) {
        synchronized (locker) {
            locker.set(true);
            selector.wakeup();
            try {
                if (channel.isRegistered()) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        //erase all interest
                        key.cancel();
                        map.remove(key);
                    }
                }
            } finally {
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static SelectionKey registerSelector(SocketChannel channel, Selector selector,
                                                 int registerOps, AtomicBoolean locker,
                                                 Map<SelectionKey, Runnable> callBackMap, Runnable runnable) {
        synchronized (locker) {
            locker.set(true);
            try {
                //make selector.select() don't block
                selector.wakeup();
                SelectionKey key = null;
                if (channel.isRegistered()) {
                    key = channel.keyFor(selector);
                    if (key != null) {
                        // add interest
                        key.interestOps(key.interestOps() | registerOps);
                    }
                }
                //key isn't exist. channel not registered yet
                if (key == null) {
                    //channel registerSelector selector ops
                    key = channel.register(selector, registerOps);
                    //save task of key
                    callBackMap.put(key, runnable);
                }
                return key;
            } catch (ClosedChannelException|CancelledKeyException|ClosedSelectorException e) {
                return null;
            } finally {
                locker.set(false);
                locker.notify();
            }
        }
    }


    private static void handleSelection(SelectionKey key, int keyOps, Map<SelectionKey, Runnable> inputCallbackMap, ExecutorService inputHandlePool, AtomicBoolean locker) {
        synchronized (locker) {
            try {
                key.interestOps(key.interestOps() & ~keyOps);
            } catch (CancelledKeyException e) {
                return;
            }
        }
        Runnable runnable = inputCallbackMap.get(key);
        if (runnable != null && !inputHandlePool.isShutdown()) {
            //sync execute
            inputHandlePool.execute(runnable);
        }
    }

    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            inputHandlePool.shutdown();
            outputHandlePool.shutdown();
            inputCallbackMap.clear();
            outputCallbackMap.clear();
            CloseUtil.close(readSelector, writeSelector);
        }
    }

    class SelectorThread extends Thread {
        private final AtomicBoolean closed;
        private final AtomicBoolean locker;
        private final Selector selector;
        private final Map<SelectionKey, Runnable> callMap;
        private final ExecutorService threadPool;
        private final int keyOps;

        public SelectorThread(String name, AtomicBoolean closed, AtomicBoolean locker, Selector selector, Map<SelectionKey, Runnable> callMap, ExecutorService threadPool, int keyOps) {
            super(name);
            this.setPriority(Thread.MAX_PRIORITY);
            this.keyOps = keyOps;
            this.closed = closed;
            this.locker = locker;
            this.selector = selector;
            this.callMap = callMap;
            this.threadPool = threadPool;
        }

        @Override
        public void run() {
            super.run();
            AtomicBoolean closed = this.closed;
            AtomicBoolean locker = this.locker;
            Selector selector = this.selector;
            Map<SelectionKey, Runnable> callMap = this.callMap;
            ExecutorService threadPool = this.threadPool;
            int keyOps = this.keyOps;
            while (!closed.get()) {
                try {
                    int select = selector.select();
                    waitSelection(locker);
                    if (select == 0) continue;
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isValid()) {
                            handleSelection(selectionKey, keyOps, callMap, threadPool, locker);
                        }
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClosedSelectorException ignored) {
                    break;
                }
            }
        }
    }
}
