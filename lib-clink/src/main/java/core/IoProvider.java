package core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {

    boolean registerInput(SocketChannel channel, HandleInputTask callback);

    boolean registerOutput(SocketChannel channel, HandleOutputTask callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class HandleInputTask implements Runnable {
        public void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    abstract class HandleOutputTask implements Runnable {

        public void run() {
            canProviderOutput();
        }

        protected abstract void canProviderOutput();
    }
}
