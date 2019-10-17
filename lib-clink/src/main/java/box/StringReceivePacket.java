package box;

import core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StringReceivePacket extends ReceivePacket<ByteArrayOutputStream> {
    private String string;

    public StringReceivePacket(int len) {
        setLength(len);
    }

    public String string(){
        return string;
    }

    @Override
    protected void closeStream() throws IOException {
        super.closeStream();
        string = new String(stream.toByteArray());
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) getLength());
    }
}
