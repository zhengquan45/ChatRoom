package org.zhq.frames;

import org.zhq.core.Frame;
import org.zhq.core.IoArgs;

import java.io.IOException;

public abstract class AbsSendFrame extends Frame {
    protected volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;

    protected volatile int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        this.bodyRemaining = length;
    }

    public AbsSendFrame(byte[] head) {
        super(head);
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        try {
            ioArgs.limit(headerRemaining + bodyRemaining);
            ioArgs.startWriting();
            if (headerRemaining > 0 && ioArgs.remained()) {
                headerRemaining -= consumeHeader(ioArgs);
            }
            if (headerRemaining == 0 && bodyRemaining > 0 && ioArgs.remained()) {
                bodyRemaining -= consumeBody(ioArgs);
            }
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            ioArgs.finishWriting();
        }
    }

    protected byte consumeHeader(IoArgs ioArgs) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) ioArgs.readFrom(header, offset, count);
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;


}
