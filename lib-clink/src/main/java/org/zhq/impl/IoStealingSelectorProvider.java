package org.zhq.impl;

import org.zhq.core.IoProvider;
import org.zhq.impl.stealing.IoTask;
import org.zhq.impl.stealing.StealingSelectorThread;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class IoStealingSelectorProvider implements IoProvider {

    private final StealingSelectorThread thread;


    public IoStealingSelectorProvider(int poolSize) throws IOException {
        Selector selector = Selector.open();
        thread = new StealingSelectorThread(selector) {
            @Override
            protected boolean processTask(IoTask task) {
                task.providerTask.run();
                return false;
            }
        };
        thread.start();
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleProviderTask callback) {
        return thread.register(channel, SelectionKey.OP_READ,callback);
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleProviderTask callback) {
        return thread.register(channel, SelectionKey.OP_WRITE,callback);
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        thread.unregister(channel);
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {

    }

    @Override
    public void close() throws IOException {
        thread.exit();
    }
}
