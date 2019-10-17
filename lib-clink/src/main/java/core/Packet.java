package core;

import java.io.Closeable;
import java.io.IOException;

/**
 * type length only readFrom.
 */
public abstract class Packet<T extends Closeable> implements Closeable {

    private byte type;
    private long length;
    protected T stream;
    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    protected abstract T createStream();

    public final T open() {
        if (stream == null) {
            stream = createStream();
        }
        return stream;
    }


    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream();
            stream = null;
        }
    }

    protected void closeStream() throws IOException {
        stream.close();
    }
}
