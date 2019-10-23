package frames;

import core.IoArgs;

public class ReceiveHeaderFrame extends AbsReceiveFrame {
    private final byte[] body;

    public ReceiveHeaderFrame(byte[] head) {
        super(head);
        body = new byte[bodyRemaining];
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        int offset = body.length - bodyRemaining;
        return ioArgs.writeTo(body, offset);
    }

    public long getPacketLength() {
        return (((long) body[0] & 0xFFL) << 32) |
                (((long) body[1] & 0xFFL) << 24) |
                (((long) body[2] & 0xFFL) << 16) |
                (((long) body[3] & 0xFFL) << 8) |
                ((long) body[4] & 0xFFL);
    }

    public byte getPacketType() {
        return body[5];
    }

    public byte[] getPacketHeaderInfo() {
        if (body.length > SendHeaderFrame.PACKET_HEADER_FRAME_MIN_LENGTH) {
            byte[] headerInfo = new byte[body.length - SendHeaderFrame.PACKET_HEADER_FRAME_MIN_LENGTH];
            System.arraycopy(body, SendHeaderFrame.PACKET_HEADER_FRAME_MIN_LENGTH, headerInfo, 0, headerInfo.length);
            return headerInfo;
        }
        return null;
    }
}
