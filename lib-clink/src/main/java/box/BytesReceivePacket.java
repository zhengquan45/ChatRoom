package box;

import java.io.ByteArrayOutputStream;

public class BytesReceivePacket extends AbsByteArrayReceivePacket<byte[]> {

    public BytesReceivePacket(int len) {
        super(len);
    }

    @Override
    protected byte[] buildEntity(ByteArrayOutputStream stream) {
        return stream.toByteArray();
    }

    @Override
    protected byte type() {
        return TYPE_MEMORY_BYTES;
    }
}
