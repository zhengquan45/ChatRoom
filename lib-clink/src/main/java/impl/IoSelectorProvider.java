package impl;

import core.IoProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
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
        this.inputHandlePool = Executors.newFixedThreadPool(4);
        this.outputHandlePool = Executors.newFixedThreadPool(4);
        startRead();
        startWrite();
    }

    private void startWrite() {
        Thread writeThread = new Thread("Write Thread Listener") {
            @Override
            public void run() {
                while (!closed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(inRegOutput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
    }

    private void startRead() {
        Thread readThread = new Thread("Read Thread Listener") {
            @Override
            public void run() {
                while (!closed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(inRegInput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        readThread.setPriority(Thread.MAX_PRIORITY);
        readThread.start();
    }

    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return registerSelector(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callback) != null;
    }

    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return registerSelector(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callback) != null;
    }

    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap);
    }


    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap);
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

    private static void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> map) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                //erase all interest
                key.cancel();
                map.remove(key);
                selector.wakeup();
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
            } catch (Exception e) {
                return null;
            } finally {
                locker.set(false);
                locker.notify();
            }
        }
    }


    private static void handleSelection(SelectionKey key, int keyOps, Map<SelectionKey, Runnable> inputCallbackMap, ExecutorService inputHandlePool) {
        key.interestOps(key.interestOps() & ~keyOps);
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
            readSelector.wakeup();
            writeSelector.wakeup();
        }
    }
}
