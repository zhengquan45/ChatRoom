package org.zhq.impl;

import org.zhq.core.IoProvider;
import org.zhq.impl.stealing.IoTask;
import org.zhq.impl.stealing.StealingSelectorThread;
import org.zhq.impl.stealing.StealingService;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class IoStealingSelectorProvider implements IoProvider {

    private final StealingSelectorThread[]threads;

    private final StealingService stealingService;


    public IoStealingSelectorProvider(int poolSize) throws IOException {
        threads = new StealingSelectorThread[poolSize];
        for (int i = 0; i < poolSize; i++) {
            Selector selector = Selector.open();
            threads[i] = new IoStealingThread("IoStealing-Thread-"+i,selector);
        }
        StealingService stealingService = new StealingService(10,threads);
        for (StealingSelectorThread thread : threads) {
            thread.setStealingService(stealingService);
            thread.start();
        }
        this.stealingService = stealingService;

    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleProviderTask callback) {
        return stealingService.getNotBusyThread().register(channel, SelectionKey.OP_READ,callback);
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleProviderTask callback) {
        return stealingService.getNotBusyThread().register(channel, SelectionKey.OP_WRITE,callback);
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        for (StealingSelectorThread thread : threads) {
            thread.unregister(channel);
        }
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        //do nothing
    }

    @Override
    public void close() throws IOException {
        stealingService.shutdown();
    }

    static class IoStealingThread extends StealingSelectorThread{

        protected IoStealingThread(String name,Selector selector) {
            super(selector);
            setName(name);
        }

        @Override
        protected boolean processTask(IoTask task) {
            task.providerTask.run();
            return false;
        }
    }
}
