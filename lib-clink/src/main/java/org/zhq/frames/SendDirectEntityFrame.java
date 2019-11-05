package org.zhq.frames;

import org.zhq.core.Frame;
import org.zhq.core.IoArgs;
import org.zhq.core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class SendDirectEntityFrame extends AbsSendPacketFrame{
private final ReadableByteChannel channel;

    public SendDirectEntityFrame(short identifier, int available, ReadableByteChannel channel, SendPacket<?> packet) {
        super(Math.min(available,Frame.MAX_CAPACITY),Frame.TYPE_PACKET_ENTITY,Frame.FLAG_NONE,identifier,packet);
        this.channel = channel;
    }

    public static Frame buildEntityFrame(SendPacket<?> packet, short identifier) {

        int available = packet.available();
        if (available == 0) {
            return new CancelSendFrame(identifier);
        }
        InputStream inputStream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        return new SendDirectEntityFrame(identifier,available,channel,packet);
    }

    @Override
    protected Frame buildNextFrame() {
        int available = packet.available();
        if (available == 0) {
            return new CancelSendFrame(getIdentifier());
        }
        return new SendDirectEntityFrame(getIdentifier(),available,channel,packet);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        if (packet == null) {
            return ioArgs.fillEmpty(bodyRemaining);
        }
        return ioArgs.readFrom(channel);
    }
}
