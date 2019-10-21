package frames;

import core.Frame;
import core.IoArgs;
import core.SendPacket;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class SendHeaderFrame extends AbsSendPacketFrame {
    public static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private final byte[] body;


    public SendHeaderFrame(short identifier, SendPacket packet) {
        super(PACKET_HEADER_FRAME_MIN_LENGTH,
                Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE,
                identifier, packet);
        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeadInfo = packet.headInfo();
        body = new byte[bodyRemaining];
        body[0] = (byte) (packetLength >> 32);
        body[1] = (byte) (packetLength >> 24);
        body[2] = (byte) (packetLength >> 16);
        body[3] = (byte) (packetLength >> 8);
        body[4] = (byte) (packetLength);
        body[5] = packetType;

        if(packetHeadInfo!=null){
            System.arraycopy(packetHeadInfo,0,
                    body,PACKET_HEADER_FRAME_MIN_LENGTH,packetHeadInfo.length);
        }
    }


    @Override
    protected int consumeBody(IoArgs ioArgs) {
        int count = bodyRemaining;
        int offset = body.length - count;
        return ioArgs.readFrom(body, offset, count);
    }

    @Override
    public Frame nextFrame() {
        InputStream stream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(stream);

        return new SendEntityFrame(getIdentifier(),packet.length(),channel,packet);
    }


}
