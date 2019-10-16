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

    private IoArgs ioArgs = new IoArgs();
    private SendPacket curSendPacket;
    private int total;
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
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
        total = curSendPacket.getLength();
        position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        ioArgs.startWriting();
        if (position >= total) {
            sendNextPacket();
            return;
        } else if (position == 0) {
            //首包
            ioArgs.writeLength(total);
        }
        //拆包
        byte[] bytes = curSendPacket.bytes();
        int count = ioArgs.readFrom(bytes, position);
        position += count;
        ioArgs.finishWriting();
        try {
            sender.sendAsync(ioArgs, ioArgsEventListener);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void cancel(SendPacket sendPacket) {

    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {

        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            sendCurrentPacket();
        }
    };

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            sending.set(false);
            if (curSendPacket != null) {
                CloseUtil.close(curSendPacket);
                curSendPacket = null;
            }
        }
    }
}
