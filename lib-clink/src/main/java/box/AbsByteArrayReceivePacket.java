package box;

import core.ReceivePacket;

import java.io.ByteArrayOutputStream;
public abstract class AbsByteArrayReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream,Entity> {
    public AbsByteArrayReceivePacket(int len) {
        super(len);
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length());
    }
}
