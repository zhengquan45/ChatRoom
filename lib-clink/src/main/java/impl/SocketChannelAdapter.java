package impl;

import core.IoArgs;
import core.IoProvider;
import core.Receiver;
import core.Sender;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

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
        return ioProvider.registerOutput(channel, handleOutputTask);
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
            if (args == null) {
                args = receiveProcessor.provideIoArgs();
            }
            try {
                if (args == null) {
                    receiveProcessor.onConsumeFailed(null, new IOException("provideIoArgs is null"));
                }
                int count = args.readFrom(channel);
                if (count == 0) {
                    log.error("current read zero data!");
                }
                if (args.remained()) {
                    attach = args;
                    ioProvider.registerInput(channel,this);
                } else {
                    receiveProcessor.onConsumeCompleted(args);
                }
            } catch (IOException e) {
                log.info("channel readFrom exception:", e);
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
            if (args == null) {
                args = sendProcessor.provideIoArgs();
            }
            try {
                if (args == null) {
                    sendProcessor.onConsumeFailed(null, new IOException("provideIoArgs is null"));
                }
                int count = args.writeTo(channel);
                if (count == 0) {
                    log.error("current write zero data!");
                }
                if (args.remained()) {
                    attach = args;
                    ioProvider.registerOutput(channel, this);
                } else {
                    sendProcessor.onConsumeCompleted(args);
                }
            } catch (IOException e) {
                log.info("channel writeTo exception:", e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };
}
