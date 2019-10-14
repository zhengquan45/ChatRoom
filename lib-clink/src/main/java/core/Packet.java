package core;

import java.io.Closeable;
import java.io.IOException;

/**
 * type length only readFrom.
 */
public abstract class Packet implements Closeable {

    private byte type;
    private int length;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
