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
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendProcessor(sendProcessor);
        reader = new AsyncPacketReader(sendPacketProvider);
    }

    @Override
    public void send(SendPacket sendPacket) {
        synchronized (queueLock) {
            queue.offer(sendPacket);
            if (sending.compareAndSet(false, true)) {
                if (reader.requestTakePacket()) {
                    requestSend();
                }
            }
        }
    }


    private void requestSend() {
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }


    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            sending.set(false);
            reader.close();
        }
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean ret;
        synchronized (queueLock) {
            ret = queue.remove(packet);
        }
        if (ret) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    private final IoArgs.IoArgsEventProcessor sendProcessor = new IoArgs.IoArgsEventProcessor() {

        @Override
        public IoArgs provideIoArgs() {

           return reader.fillData();
//            if (channel == null) {
//                channel = Channels.newChannel(curSendPacket.open());
//                ioArgs.limit(4);
////                ioArgs.writeLength((int) curSendPacket.length());
//            } else {
//                ioArgs.limit((int) Math.min(total - position, ioArgs.capacity()));
//                try {
//                    int count = ioArgs.readFrom(channel);
//                    position += count;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return null;
//                }
//            }
//            return ioArgs;
        }

        @Override
        public void onConsumeFailed(IoArgs ioArgs, Exception e) {
            if(ioArgs!=null) {
                e.printStackTrace();
            }else{
                //TODO
            }
        }

        @Override
        public void onConsumeCompleted(IoArgs ioArgs) {
            if(reader.requestTakePacket()){
                requestSend();
            }
        }
    };

    private final AsyncPacketReader.PacketProvider sendPacketProvider = new AsyncPacketReader.PacketProvider() {
        @Override
        public SendPacket takePacket() {
            SendPacket packet;
            synchronized (queueLock) {
                packet = queue.poll();
                if(packet==null) {
                    sending.set(false);
                    return null;
                }
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
