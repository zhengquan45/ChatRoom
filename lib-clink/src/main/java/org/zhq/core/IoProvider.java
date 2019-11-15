package org.zhq.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {

    void register(HandleProviderTask callback)throws Exception;

    void unRegister(SocketChannel channel);

    abstract class HandleProviderTask extends IoTask implements Runnable {
        protected volatile IoArgs attach;

        private IoProvider ioProvider;

        public HandleProviderTask(SocketChannel channel, int ops, IoProvider ioProvider) {
            super(channel, ops);
            this.ioProvider = ioProvider;
        }

        @Override
        public final void run() {
            if(onProviderIo(attach)){
                try {
                    ioProvider.register(this);
                } catch (Exception e) {
                    fireThrowable(e);
                }
            }
        }

        protected abstract boolean onProviderIo(IoArgs ioArgs);

        public void checkAttachNull(){
            if(attach!=null){
                throw new IllegalStateException("Current attach is not empty!");
            }
        }
    }
}
