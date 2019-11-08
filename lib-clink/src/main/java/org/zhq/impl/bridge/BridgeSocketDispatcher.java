package org.zhq.impl.bridge;

import org.zhq.core.*;
import org.zhq.utils.plugin.CircularByteBuffer;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class BridgeSocketDispatcher implements SendDispatcher, ReceiveDispatcher {
    private final CircularByteBuffer buffer = new CircularByteBuffer(512, true);
    private final ReadableByteChannel readChannel = Channels.newChannel(buffer.getInputStream());
    private final WritableByteChannel writeChannel = Channels.newChannel(buffer.getOutputStream());
    private final IoArgs receiveIoArgs = new IoArgs(256,false);
    private final Receiver receiver;
    private final AtomicBoolean sending = new AtomicBoolean();
    private final IoArgs sendIoArgs = new IoArgs();
    private volatile Sender sender;

    public BridgeSocketDispatcher(Receiver receiver) {
        this.receiver = receiver;
    }

    public void bindSender(Sender sender) {
        if (this.sender != null) {
            this.sender.setSendProcessor(null);
        }
        synchronized (sending) {
            sending.set(false);
        }
        buffer.clear();
        this.sender = sender;
        if (this.sender != null) {
            this.sender.setSendProcessor(senderEventProcessor);
            requestSend();
        }
    }

    @Override
    public void start() {
        receiver.setReceiveProcessor(receiveEventProcessor);
        registerReceive();
    }

    @Override
    public void stop() {
        // nothing
    }

    @Override
    public void send(SendPacket sendPacket) {
        // nothing
    }

    @Override
    public void sendHeartBeat() {
        // nothing
    }

    @Override
    public void cancel(SendPacket sendPacket) {
        // nothing
    }

    @Override
    public void close() throws IOException {
        //nothing
    }

    private void requestSend() {
        synchronized (sending) {
            Sender sender = this.sender;
            if (sending.get() || sender == null) {
                return;
            }
            if (buffer.getAvailable() > 0) {
                try {
                    boolean succeed = sender.postSendAsync();
                    if (succeed) {
                        sending.set(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final IoArgs.IoArgsEventProcessor senderEventProcessor = new IoArgs.IoArgsEventProcessor() {
        @Override
        public IoArgs provideIoArgs() {
            int available = buffer.getAvailable();
            if(available>0){
                sendIoArgs.limit(available);
                sendIoArgs.startWriting();
                try {
                    sendIoArgs.readFrom(readChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendIoArgs.finishWriting();
                return sendIoArgs;
            }
            return null;
        }

        @Override
        public void onConsumeFailed(Exception e) {
            e.printStackTrace();
            synchronized (sending){
                sending.set(false);
            }
            requestSend();
        }

        @Override
        public void onConsumeCompleted(IoArgs ioArgs) {
            synchronized (sending){
                sending.set(false);
            }
            requestSend();
        }
    };

    private final IoArgs.IoArgsEventProcessor receiveEventProcessor = new IoArgs.IoArgsEventProcessor() {
        @Override
        public IoArgs provideIoArgs() {
            receiveIoArgs.resetLimit();
            receiveIoArgs.startWriting();
            return receiveIoArgs;
        }

        @Override
        public void onConsumeFailed(Exception e) {
            e.printStackTrace();
        }

        @Override
        public void onConsumeCompleted(IoArgs ioArgs) {
            ioArgs.finishWriting();
            try {
                ioArgs.writeTo(writeChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
            registerReceive();
            requestSend();
        }
    };

}
