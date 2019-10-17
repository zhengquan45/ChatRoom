package box;

import java.io.ByteArrayOutputStream;

public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {

    public StringReceivePacket(int len) {
        super(len);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }

    @Override
    protected byte type() {
        return TYPE_MEMORY_STRING;
    }
}
