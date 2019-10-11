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
    private final onChannelStatusChangedListener Listener;
    private IoArgs.IoArgsEventListener receiveListener;
    private IoArgs.IoArgsEventListener sendListener;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, onChannelStatusChangedListener Listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.Listener = Listener;
        channel.configureBlocking(false);
    }

    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
        if(closed.get()){
            throw new IOException("current channel is already closed.");
        }
        receiveListener = listener;
        return ioProvider.registerInput(channel,handleInputCallback);
    }

    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        if(closed.get()){
            throw new IOException("current channel is already closed.");
        }
        sendListener = listener;
        handleOutputCallback.setAttach(args);
        return ioProvider.registerOutput(channel,handleOutputCallback);
    }

    public void close() throws IOException {
        if(closed.compareAndSet(false,true)){
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            CloseUtil.close(channel);
        }
    }

    public interface onChannelStatusChangedListener{
        void onChannelClosed(SocketChannel channel);
    }

    /**
     * this is a runnable,a task for .read data from channel to IoArgs.
     */
    private final IoProvider.HandleInputCallback handleInputCallback = new IoProvider.HandleInputCallback() {
        protected void canProviderInput() {
            if(closed.get()){
                return;
            }
            IoArgs args = new IoArgs();
            IoArgs.IoArgsEventListener receiveListener = SocketChannelAdapter.this.receiveListener;
            if(receiveListener!=null) {
                receiveListener.onStarted(args);
            }
            try {
                if(args.read(channel)>0 && receiveListener!=null){
                    receiveListener.onCompleted(args);
                }else{
                    throw new IOException("channel can't read data");
                }
            } catch (IOException e) {
                log.info("channel read exception:",e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleOutputCallback handleOutputCallback = new IoProvider.HandleOutputCallback() {
        protected void canProviderOutput(Object attach) {

        }
    };
}
