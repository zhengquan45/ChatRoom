package core;

import box.StringReceivePacket;
import box.StringSendPacket;
import impl.SocketChannelAdapter;
import impl.async.AsyncReceiveDispatcher;
import impl.async.AsyncSendDispatcher;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;


@Slf4j
public class Connector implements Closeable, SocketChannelAdapter.onChannelStatusChangedListener {
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
        IoContext ioContext = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, ioContext.getIoProvider(), this);
        sender = adapter;
        receiver = adapter;
        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver,echoListener);
        receiveDispatcher.start();
    }



    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    @Override
    public void close() throws IOException {
        CloseUtil.close(receiveDispatcher,sendDispatcher,sender,receiver,channel);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    private ReceiveDispatcher.ReceivePacketCallBack echoListener = new ReceiveDispatcher.ReceivePacketCallBack() {

        @Override
        public void onReceivePacketCompleted(ReceivePacket receivePacket) {
            if(receivePacket instanceof StringReceivePacket){
                String msg = ((StringReceivePacket) receivePacket).buffer2String();
                onReceiveNewMessage(msg);
            }
        }
    };

    protected void onReceiveNewMessage(String msg) {
        log.info("{}:{}", key.toString(), msg);
    }
}
