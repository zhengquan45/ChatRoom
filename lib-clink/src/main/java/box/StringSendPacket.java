package box;

import core.SendPacket;

import java.io.IOException;

public class StringSendPacket extends SendPacket {
    private final byte[]bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        setLength(bytes.length);
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public void close() throws IOException {

    }
}
