package org.zhq.impl.async;

import org.zhq.core.*;
import org.zhq.utils.CloseUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallBack callBack;

    private final AsyncPacketWriter writer;
    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallBack callBack) {
        this.receiver = receiver;
        this.receiver.setReceiveProcessor(receiveProcessor);
        this.callBack = callBack;
        this.writer = new AsyncPacketWriter(provider);
    }

    @Override
    public void start() {
        registerReceive();
    }


    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            CloseUtil.close(writer);
            receiver.setReceiveProcessor(null);
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }



    private final IoArgs.IoArgsEventProcessor receiveProcessor = new IoArgs.IoArgsEventProcessor() {

        @Override
        public IoArgs provideIoArgs() {
            IoArgs ioArgs = writer.takeIoArgs();
            ioArgs.startWriting();
            return ioArgs;
        }

        @Override
        public void onConsumeFailed(Exception e) {
            CloseUtil.close(AsyncReceiveDispatcher.this);
        }

        @Override
        public void onConsumeCompleted(IoArgs ioArgs) {
            ioArgs.finishWriting();
            while (!closed.get() && ioArgs.remained()){
                writer.consumeIoArgs(ioArgs);
            }
            registerReceive();
        }
    };

    private final AsyncPacketWriter.PacketProvider provider = new AsyncPacketWriter.PacketProvider() {


        @Override
        public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
            return callBack.onArrivedNewPacket(type,length,headerInfo);
        }

        @Override
        public void completedPacket(ReceivePacket packet, boolean succeed) {
            CloseUtil.close(packet);
            callBack.onReceivePacketCompleted(packet);
        }

        @Override
        public void onReceivedHeartBeat() {
            callBack.onReceivedHeartBeat();
        }
    };


}
