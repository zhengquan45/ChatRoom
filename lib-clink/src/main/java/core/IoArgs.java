package core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class IoArgs {
    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(limit);

    public int readFrom(ReadableByteChannel channel) throws IOException {
        startWriting();
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        finishWriting();
        return bytesProduced;
    }

    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }


    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        finishWriting();
        return bytesProduced;
    }

    public int writeTo(SocketChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }


    public void startWriting() {
        buffer.clear();
        buffer.limit(limit);
    }

    public void limit(int limit){
        this.limit = limit;
    }

    public void finishWriting() {
        buffer.flip();
    }

    public void writeLength(int total) {
        startWriting();
        buffer.putInt(total);
        finishWriting();
    }
    public Integer readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public interface IoArgsEventProcessor {
       IoArgs provideIoArgs();

       void onConsumeFailed(IoArgs ioArgs,Exception e);

       void onConsumeCompleted(IoArgs ioArgs);
    }
}
