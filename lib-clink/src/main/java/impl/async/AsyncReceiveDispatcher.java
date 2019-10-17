package impl.async;

import box.StringReceivePacket;
import core.IoArgs;
import core.ReceiveDispatcher;
import core.ReceivePacket;
import core.Receiver;
import utils.CloseUtil;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallBack callBack;

    private IoArgs args = new IoArgs();
    private ReceivePacket<?,?> curReceivePacket;
    private WritableByteChannel channel;
    private long total;
    private long position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallBack callBack) {
        this.receiver = receiver;
        this.receiver.setReceiveProcessor(receiveProcessor);
        this.callBack = callBack;
    }

    @Override
    public void start() {
        registerReceive();
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

    @Override
    public void stop() {

    }

    private final IoArgs.IoArgsEventProcessor receiveProcessor = new IoArgs.IoArgsEventProcessor() {

        @Override
        public IoArgs provideIoArgs() {
            int receiveSize;
            if (curReceivePacket == null) {
                receiveSize = 4;
            } else {
                receiveSize = (int) Math.min(total - position, args.capacity());
            }
            args.limit(receiveSize);
            return args;
        }

        @Override
        public void onConsumeFailed(IoArgs ioArgs, Exception e) {
            e.printStackTrace();
        }

        @Override
        public void onConsumeCompleted(IoArgs ioArgs) {
            assemblePacket(args);
            registerReceive();
        }
    };

    private void assemblePacket(IoArgs args) {
        //包头
        if (curReceivePacket == null) {
            int length = args.readLength();
            curReceivePacket = new StringReceivePacket(length);
            channel = Channels.newChannel(curReceivePacket.open());
            total = length;
            position = 0;
        }
        try {
            int count = args.writeTo(channel);
            position += count;
            if (position == total) {
                completePacket(true);
                curReceivePacket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            completePacket(false);
        }

    }

    private void completePacket(boolean succeed) {
        CloseUtil.close(curReceivePacket, channel);
        callBack.onReceivePacketCompleted(curReceivePacket);
        curReceivePacket = null;
        channel = null;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            completePacket(false);
        }
    }
}
