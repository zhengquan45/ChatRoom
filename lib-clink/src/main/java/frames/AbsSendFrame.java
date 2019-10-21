package frames;

import core.Frame;
import core.IoArgs;

import java.io.IOException;

public abstract class AbsSendFrame extends Frame {
    protected volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;

    protected volatile int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
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

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;

    protected byte consumeHeader(IoArgs ioArgs) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) ioArgs.readFrom(header, offset, count);
    }

    protected synchronized boolean isSending(){
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }
}
