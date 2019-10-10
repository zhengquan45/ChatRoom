package core;

import java.io.Closeable;
import java.io.IOException;

public class IoContext implements Closeable {

    private static IoContext INSTANCE;
    private final IoProvider ioProvider;

    public IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public static IoContext get() {
        return INSTANCE;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }


    public static StartedBoot setUp(){
        return new StartedBoot();
    }

    public void close() throws IOException {
        ioProvider.close();
    }

    public static class StartedBoot{
        private IoProvider ioProvider;

        private StartedBoot() {
        }

        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start(){
            INSTANCE= new IoContext(ioProvider);
            return INSTANCE;
        }
    }
}
