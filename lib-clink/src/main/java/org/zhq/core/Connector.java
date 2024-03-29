package org.zhq.core;

import org.zhq.box.*;
import org.zhq.impl.SocketChannelAdapter;
import org.zhq.impl.async.AsyncReceiveDispatcher;
import org.zhq.impl.async.AsyncSendDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.zhq.impl.bridge.BridgeSocketDispatcher;
import org.zhq.utils.CloseUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
public abstract class Connector implements Closeable, SocketChannelAdapter.onChannelStatusChangedListener {
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;
    private final List<ScheduleJob> scheduleJobList = new ArrayList<>(4);

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

    public Sender getSender() {
        return sender;
    }

    public long getLastActiveTime(){
        return Math.max(sender.getLastWriteTime(),receiver.getLastReadTime());
    }

    public void send(String msg) {
        log.info("发送信息",msg);
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    public void send(SendPacket sendPacket) {
        sendDispatcher.send(sendPacket);
    }

    public void changeToBridge(){
        if(receiveDispatcher instanceof BridgeSocketDispatcher){
            return;
        }
        receiveDispatcher.stop();
        BridgeSocketDispatcher dispatcher = new BridgeSocketDispatcher(receiver);
        receiveDispatcher = dispatcher;
        dispatcher.start();
    }

    public void bindToBridge(Sender sender){
        if(this.sender == sender){
            throw new UnsupportedOperationException("Can not set current connector sender");
        }
        if(!(receiveDispatcher instanceof BridgeSocketDispatcher)){
            throw new IllegalStateException("receiveDispatcher isn't bridgeSocketDispatcher");
        }
        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(sender);
    }

    public void unBindToBridge(){
        if(!(receiveDispatcher instanceof BridgeSocketDispatcher)){
            throw new IllegalStateException("receiveDispatcher isn't bridgeSocketDispatcher");
        }
        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(null);
    }

    @Override
    public void close() throws IOException {
        CloseUtil.close(receiveDispatcher, sendDispatcher, sender, receiver, channel);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        synchronized (scheduleJobList){
            for (ScheduleJob scheduleJob : scheduleJobList) {
                scheduleJob.unSchedule();
            }
            scheduleJobList.clear();
        }
        CloseUtil.close(this);
    }

    private ReceiveDispatcher.ReceivePacketCallBack echoListener = new ReceiveDispatcher.ReceivePacketCallBack() {

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length,byte[]headerInfo) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_MEMORY_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile(length,headerInfo));
                case Packet.TYPE_MEMORY_DIRECT:
                    return new StreamDirectReceivePacket(createNewReceiveDirectOutputStream(length,headerInfo),length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type:" + type);
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket receivePacket) {
            onReceivedNewPacket(receivePacket);
        }

        @Override
        public void onReceivedHeartBeat() {
            log.info("{}:[HEARTBEAT]",key.toString());
        }
    };

    protected abstract OutputStream createNewReceiveDirectOutputStream(long length, byte[] headerInfo);

    protected abstract File createNewReceiveFile(long length, byte[] headerInfo);


    protected void onReceivedNewPacket(ReceivePacket packet) {
//        log.info("{}:[New Packet]-Type:{},length:{}", key.toString(), packet.type(), packet.length());
    }

    public void schedule(ScheduleJob scheduleJob){
        synchronized (scheduleJobList) {
           if(scheduleJobList.contains(scheduleJob)){
               return;
           }
           Scheduler scheduler = IoContext.get().getScheduler();
           scheduleJob.schedule(scheduler);
           scheduleJobList.add(scheduleJob);
        }
    }

    public void fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartBeat();
    }

    public void fireExceptionCaught(Throwable e) {

    }
}
