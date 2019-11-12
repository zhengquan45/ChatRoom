package org.zhq.impl;

import org.zhq.core.IoArgs;
import org.zhq.core.IoProvider;
import org.zhq.core.Receiver;
import org.zhq.core.Sender;
import lombok.extern.slf4j.Slf4j;
import org.zhq.utils.CloseUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final onChannelStatusChangedListener listener;
    private IoArgs.IoArgsEventProcessor receiveProcessor;
    private IoArgs.IoArgsEventProcessor sendProcessor;
    private volatile long lastWriteTime = System.currentTimeMillis();
    private volatile long lastReadTime = System.currentTimeMillis();


    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, onChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;
        channel.configureBlocking(false);
    }

    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            CloseUtil.close(channel);
            listener.onChannelClosed(channel);
        }
    }

    @Override
    public void setReceiveProcessor(IoArgs.IoArgsEventProcessor processor) {
        receiveProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (closed.get()) {
            throw new IOException("current channel is already closed.");
        }
        handleInputTask.checkAttachNull();
        return ioProvider.registerInput(channel, handleInputTask);
    }

    @Override
    public void setSendProcessor(IoArgs.IoArgsEventProcessor processor) {
        sendProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (closed.get()) {
            throw new IOException("current channel is already closed.");
        }
        handleOutputTask.checkAttachNull();
//        ioProvider.registerOutput(channel, handleOutputTask);
        handleOutputTask.run();
        return true;
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }

    public interface onChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }

    /**
     * this is a runnable,a task for .readFrom data from channel to IoArgs.
     */
    private final IoProvider.HandleProviderTask handleInputTask = new IoProvider.HandleProviderTask() {
        @Override
        protected void onProviderTo(IoArgs args) {
            if (closed.get()) {
                return;
            }
            lastReadTime = System.currentTimeMillis();
            if (receiveProcessor == null) {
                return;
            }
            if (args == null) {
                args = receiveProcessor.provideIoArgs();
            }
            try {
                if (args == null) {
                    receiveProcessor.onConsumeFailed(new IOException("provideIoArgs is null"));
                }
                int count = args.readFrom(channel);
                if (count == 0) {
                    log.error("current read zero data!");
                }
                if (args.remained() && args.isNeedConsumeRemaining()) {
                    attach = args;
                    ioProvider.registerInput(channel, this);
                } else {
                    receiveProcessor.onConsumeCompleted(args);
                }
            } catch (IOException e) {
                receiveProcessor.onConsumeFailed(e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleProviderTask handleOutputTask = new IoProvider.HandleProviderTask() {
        @Override
        protected void onProviderTo(IoArgs args) {
            if (closed.get()) {
                return;
            }
            lastWriteTime = System.currentTimeMillis();
            if (sendProcessor == null) {
                return;
            }
            if (args == null) {
                args = sendProcessor.provideIoArgs();
            }
            try {
                if (args == null) {
                    sendProcessor.onConsumeFailed(new IOException("provideIoArgs is null"));
                }
                int count = args.writeTo(channel);
                if (count == 0) {
                    log.error("current write zero data!");
                }
                if (args.remained() && args.isNeedConsumeRemaining()) {
                    attach = args;
                    ioProvider.registerOutput(channel, this);
                } else {
                    sendProcessor.onConsumeCompleted(args);
                }
            } catch (IOException e) {
                sendProcessor.onConsumeFailed(e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };
}
