package frames;

import core.Frame;
import core.IoArgs;
import core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class SendEntityFrame extends AbsSendPacketFrame {
    private final long unConsumeEntityLength;

    private final ReadableByteChannel channel;

    SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel, SendPacket packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE,
                identifier, packet);
        this.unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        if (packet == null) {
            return ioArgs.fillEmpty(bodyRemaining);
        }
        return ioArgs.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) {
            return null;
        }
        return new SendEntityFrame(getIdentifier(), unConsumeEntityLength, channel, packet);
    }
}
