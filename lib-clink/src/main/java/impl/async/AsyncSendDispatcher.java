package impl.async;

import core.IoArgs;
import core.SendDispatcher;
import core.SendPacket;
import core.Sender;
import utils.CloseUtil;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean sending = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final AsyncPacketReader reader;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendProcessor(sendProcessor);
        reader = new AsyncPacketReader(sendPacketProvider);
    }

    @Override
    public void send(SendPacket sendPacket) {
        queue.offer(sendPacket);
        requestSend();
    }


    private void requestSend() {
        synchronized (sending) {
            if (sending.get() || closed.get()) {
                return;
            }
            if(reader.requestTakePacket()){
                try {
                    boolean succeed = sender.postSendAsync();
                    if(succeed){
                        sending.set(true);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }

    }


    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            reader.close();
            queue.clear();
            synchronized (sending) {
                sending.set(false);
            }
        }
    }

    @Override
    public void cancel(SendPacket packet) {
        if (queue.remove(packet)) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    private final IoArgs.IoArgsEventProcessor sendProcessor = new IoArgs.IoArgsEventProcessor() {

        @Override
        public IoArgs provideIoArgs() {
            return closed.get()?null:reader.fillData();
        }

        @Override
        public void onConsumeFailed(IoArgs ioArgs, Exception e) {
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

    private final AsyncPacketReader.PacketProvider sendPacketProvider = new AsyncPacketReader.PacketProvider() {
        @Override
        public SendPacket takePacket() {
            SendPacket packet = queue.poll();
            if (packet == null) {
                return null;
            }
            if (packet.isCanceled()) {
                return takePacket();
            }
            return packet;
        }

        @Override
        public void completedPacket(SendPacket packet, boolean succeed) {
            CloseUtil.close(packet);
        }
    };


}
