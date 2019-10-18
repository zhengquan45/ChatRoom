package frames;

import core.Frame;
import core.IoArgs;
import core.SendPacket;

import java.nio.channels.ReadableByteChannel;

public class SendEntityFrame extends AbsSendPacketFrame{

    public SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel,SendPacket packet) {
        super((int) Math.min(entityLength,Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE,
                identifier, packet);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
