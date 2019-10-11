package core;

import impl.SocketChannelAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;


@Slf4j
public class Connector implements Closeable ,SocketChannelAdapter.onChannelStatusChangedListener{
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
        IoContext ioContext = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel,ioContext.getIoProvider(),this);
        sender = adapter;
        receiver = adapter;
        readNextMessage();
    }

    private void readNextMessage(){
        if(receiver!=null){
            try {
                receiver.receiveAsync(echoListener);
            } catch (IOException e) {
                log.error("receive data exception:",e);
            }
        }
    }
    @Override
    public void close() throws IOException {

    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    private IoArgs.IoArgsEventListener echoListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            onReceiveNewMessage(args.buffer2String());
            readNextMessage();
        }
    };

    protected void onReceiveNewMessage(String msg) {
        log.info("{}:{}",key.toString(),msg);
    }
}
