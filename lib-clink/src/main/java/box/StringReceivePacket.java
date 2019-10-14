package box;

import core.ReceivePacket;

import java.io.IOException;

public class StringReceivePacket extends ReceivePacket {
    private byte[] buffer;
    private int position;

    public StringReceivePacket(int len) {
        this.buffer = new byte[len];
        setLength(len);
    }

    @Override
    public void save(byte[] bytes, int count) {
        System.arraycopy(bytes, 0, buffer, position, count);
        position += count;
    }

    public String buffer2String(){
        return new String(buffer);
    }

    @Override
    public void close() throws IOException {

    }
}
