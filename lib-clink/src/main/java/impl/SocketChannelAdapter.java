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
    private final IoProvider.HandleInputTask handleInputTask = new IoProvider.HandleInputTask() {
        protected void canProviderInput() {
            if (closed.get()) {
                return;
            }
            IoArgs args = receiveProcessor.provideIoArgs();
            try {
                if(args==null){
                    receiveProcessor.onConsumeFailed(null,new IOException("provideIoArgs is null"));
                }else if (args.readFrom(channel) > 0) {
                    receiveProcessor.onConsumeCompleted(args);
                } else {
                    receiveProcessor.onConsumeFailed(args,new IOException("channel can't readFrom data"));
                }
            } catch (IOException e) {
                log.info("channel readFrom exception:", e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleOutputTask handleOutputTask = new IoProvider.HandleOutputTask() {
        @Override
        protected void canProviderOutput() {
            if (closed.get()) {
                return;
            }
            IoArgs args = sendProcessor.provideIoArgs();
            try {
                if(args==null){
                    sendProcessor.onConsumeFailed(null,new IOException("provideIoArgs is null"));
                }else if (args.writeTo(channel) > 0) {
                    sendProcessor.onConsumeCompleted(args);
                } else {
                    sendProcessor.onConsumeFailed(args,new IOException("channel can't writeTo data"));
                }
            } catch (IOException e) {
                log.info("channel writeTo exception:", e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };
}
