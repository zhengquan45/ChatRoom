package impl.async;

import core.IoArgs;
import core.SendDispatcher;
import core.SendPacket;
import core.Sender;
import utils.CloseUtil;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean sending = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private IoArgs ioArgs = new IoArgs();
    private SendPacket<?> curSendPacket;
    private ReadableByteChannel channel;
    private long total;
    private long position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendProcessor(sendProcessor);
    }

    @Override
    public void send(SendPacket sendPacket) {
        queue.offer(sendPacket);
        if (sending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }

    private SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet != null && packet.isCanceled()) {
            return takePacket();
        }
        return packet;
    }

    private void sendNextPacket() {
        if (curSendPacket != null) {
            CloseUtil.close(curSendPacket);
        }
        curSendPacket = takePacket();
        if (curSendPacket == null) {
            sending.set(false);
            return;
        }
        total = curSendPacket.length();
        position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        if (position >= total) {
            completePacket(position == total);
            sendNextPacket();
            return;
        }
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void completePacket(boolean succeed) {
        if (curSendPacket == null) {
            return;
        }
        CloseUtil.close(curSendPacket, channel);
        curSendPacket = null;
        channel = null;
        total = 0;
        position = 0;
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void cancel(SendPacket sendPacket) {

    }

    private final IoArgs.IoArgsEventProcessor sendProcessor = new IoArgs.IoArgsEventProcessor() {

        @Override
        public IoArgs provideIoArgs() {
            if (channel == null) {
                channel = Channels.newChannel(curSendPacket.open());
                ioArgs.limit(4);
                ioArgs.writeLength((int) curSendPacket.length());
            } else {
                ioArgs.limit((int) Math.min(total - position, ioArgs.capacity()));
                try {
                    int count = ioArgs.readFrom(channel);
                    position += count;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return ioArgs;
        }

        @Override
        public void onConsumeFailed(IoArgs ioArgs, Exception e) {
            e.printStackTrace();
        }

        @Override
        public void onConsumeCompleted(IoArgs ioArgs) {
            sendCurrentPacket();
        }

    };

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            sending.set(false);
            completePacket(false);
        }
    }
}
