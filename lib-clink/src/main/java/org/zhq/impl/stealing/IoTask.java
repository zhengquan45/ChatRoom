package org.zhq.impl.stealing;

import org.zhq.core.IoProvider;

import java.nio.channels.SocketChannel;

public class IoTask {
    public final SocketChannel channel;
    public final IoProvider.HandleProviderTask providerTask;
    public final int ops;

    public IoTask(SocketChannel channel, IoProvider.HandleProviderTask providerTask, int ops) {
        this.channel = channel;
        this.providerTask = providerTask;
        this.ops = ops;
    }
}
