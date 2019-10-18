package box;

import java.io.ByteArrayOutputStream;

public class BytesReceivePacket extends AbsByteArrayReceivePacket<byte[]> {

    public BytesReceivePacket(long len) {
        super(len);
    }

    @Override
    protected byte[] buildEntity(ByteArrayOutputStream stream) {
        return stream.toByteArray();
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }
}
