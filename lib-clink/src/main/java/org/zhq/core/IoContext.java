package org.zhq.core;

import java.io.Closeable;
import java.io.IOException;

public class IoContext implements Closeable {

    private static IoContext INSTANCE;
    private final IoProvider ioProvider;
    private final Scheduler scheduler;

    public IoContext(IoProvider ioProvider, Scheduler scheduler) {
        this.ioProvider = ioProvider;
        this.scheduler = scheduler;
    }

    public static IoContext get() {
        return INSTANCE;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public static StartedBoot setUp() {
        return new StartedBoot();
    }

    @Override
    public void close() throws IOException {
        ioProvider.close();
        scheduler.close();
    }

    public static class StartedBoot {
        private IoProvider ioProvider;
        private Scheduler scheduler;
        private StartedBoot() {
        }

        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public StartedBoot scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider, scheduler);
            return INSTANCE;
        }
    }
}
