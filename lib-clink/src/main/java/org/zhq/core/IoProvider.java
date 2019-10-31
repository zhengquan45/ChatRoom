package org.zhq.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {

    boolean registerInput(SocketChannel channel, HandleProviderTask callback);

    boolean registerOutput(SocketChannel channel, HandleProviderTask callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);


    abstract class HandleProviderTask implements Runnable {
        protected volatile IoArgs attach;

        public void run() {
            onProviderTo(attach);
        }

        protected abstract void onProviderTo(IoArgs ioArgs);
    }
}
