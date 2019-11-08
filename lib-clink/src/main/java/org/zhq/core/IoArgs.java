package org.zhq.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class IoArgs {
    private volatile int limit;
    private final boolean isNeedConsumeRemaining;
    private final ByteBuffer buffer;

    public IoArgs() {
        this(256);
    }

    public IoArgs(int size) {
        this(size, true);
    }

    public IoArgs(int size, boolean isNeedConsumeRemaining) {
        this.limit = size;
        this.isNeedConsumeRemaining = isNeedConsumeRemaining;
        this.buffer = ByteBuffer.allocate(limit);
    }


    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size <= 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    public int readFrom(ReadableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
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
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;
        do {
            len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException("current read any data!");
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);
        return bytesProduced;
    }

    public int writeTo(SocketChannel channel) throws IOException {
        int bytesProduced = 0;
        int len;
        do {
            len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException("current write any data!");
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);
        return bytesProduced;
    }


    public void startWriting() {
        buffer.clear();
        buffer.limit(limit);
    }

    public void limit(int limit) {
        this.limit = Math.min(limit, buffer.capacity());
    }

    public void resetLimit() {
        this.limit = buffer.capacity();
    }

    public void finishWriting() {
        buffer.flip();
    }


    public Integer readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    public boolean isNeedConsumeRemaining() {
        return isNeedConsumeRemaining;
    }

    public int fillEmpty(int size) {
        int fillSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    public int setEmpty(int size) {
        int emptySize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + emptySize);
        return emptySize;
    }


    public interface IoArgsEventProcessor {
        IoArgs provideIoArgs();

        void onConsumeFailed(Exception e);

        void onConsumeCompleted(IoArgs ioArgs);
    }
}
