package org.zhq.frames;

import org.zhq.core.IoArgs;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class ReceiveEntityFrame extends AbsReceiveFrame {
    private WritableByteChannel channel;

    public ReceiveEntityFrame(byte[] head) {
        super(head);
    }

    public void bindPacketChannel(WritableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return channel==null?ioArgs.setEmpty(bodyRemaining):ioArgs.writeTo(channel);
    }
}
