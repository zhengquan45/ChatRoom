package core;

import java.io.Closeable;
import java.io.IOException;

/**
 * type length only readFrom.
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {

    public static final byte TYPE_MEMORY_BYTES = 1;
    public static final byte TYPE_MEMORY_STRING = 2;
    public static final byte TYPE_MEMORY_FILE = 3;
    public static final byte TYPE_MEMORY_DIRECT = 4;

    protected long length;
    protected Stream stream;

    public long length() {
        return length;
    }


    protected abstract Stream createStream();

    public abstract byte type();


    public final Stream open() {
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

    public byte[]headInfo(){
        return null;
    }
}
