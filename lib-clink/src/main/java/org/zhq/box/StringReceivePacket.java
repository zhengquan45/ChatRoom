package org.zhq.box;

import java.io.ByteArrayOutputStream;

public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {

    public StringReceivePacket(long len) {
        super(len);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
