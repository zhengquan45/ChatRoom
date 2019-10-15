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
    private IoArgs.IoArgsEventListener receiveListener;
    private IoArgs.IoArgsEventListener sendListener;

    private IoArgs args;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, onChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;
        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventListener receiveListener) {
        this.receiveListener = receiveListener;
    }

    @Override
    public boolean receiveAsync(IoArgs args) throws IOException {
        if(closed.get()){
            throw new IOException("current channel is already closed.");
        }
        this.args = args;
        return ioProvider.registerInput(channel,handleInputTask);
    }

    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener sendListener) throws IOException {
        if(closed.get()){
            throw new IOException("current channel is already closed.");
        }
        this.sendListener = sendListener;
        handleOutputTask.setAttach(args);
        return ioProvider.registerOutput(channel,handleOutputTask);
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
     * this is a runnable,a task for .readFrom data from channel to IoArgs.
     */
    private final IoProvider.HandleInputTask handleInputTask = new IoProvider.HandleInputTask() {
        protected void canProviderInput() {
            if(closed.get()){
                return;
            }
            IoArgs args = SocketChannelAdapter.this.args;
            IoArgs.IoArgsEventListener receiveListener = SocketChannelAdapter.this.receiveListener;
            receiveListener.onStarted(args);
            try {
                if(args.readFrom(channel)>0){
                    receiveListener.onCompleted(args);
                }else{
                    throw new IOException("channel can't readFrom data");
                }
            } catch (IOException e) {
                log.info("channel readFrom exception:",e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleOutputTask handleOutputTask = new IoProvider.HandleOutputTask() {
        @Override
        protected void canProviderOutput(IoArgs attach) {
            if(closed.get()){
                return;
            }
            IoArgs.IoArgsEventListener sendListener = SocketChannelAdapter.this.sendListener;
            sendListener.onStarted(attach);
            try {
                if(attach.writeTo(channel)>0){
                    sendListener.onCompleted(attach);
                }else{
                    throw new IOException("channel can't writeTo data");
                }
            } catch (IOException e) {
                log.info("channel writeTo exception:",e);
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };
}
