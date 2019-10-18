package core;

import box.BytesReceivePacket;
import box.FileReceivePacket;
import box.StringReceivePacket;
import box.StringSendPacket;
import impl.SocketChannelAdapter;
import impl.async.AsyncReceiveDispatcher;
import impl.async.AsyncSendDispatcher;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;


@Slf4j
public abstract class Connector implements Closeable, SocketChannelAdapter.onChannelStatusChangedListener {
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
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, echoListener);
        receiveDispatcher.start();
    }

    public UUID getKey() {
        return key;
    }

    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    public void send(SendPacket sendPacket) {
        sendDispatcher.send(sendPacket);
    }

    @Override
    public void close() throws IOException {
        CloseUtil.close(receiveDispatcher, sendDispatcher, sender, receiver, channel);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    private ReceiveDispatcher.ReceivePacketCallBack echoListener = new ReceiveDispatcher.ReceivePacketCallBack() {

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_MEMORY_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_MEMORY_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type:" + type);
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket receivePacket) {
            onReceivedNewPacket(receivePacket);
        }
    };

    protected abstract File createNewReceiveFile();


    protected void onReceivedNewPacket(ReceivePacket packet) {
        log.info("{}:[New Packet]-Type:{},length:{}", key.toString(), packet.type(), packet.length());
    }
}
