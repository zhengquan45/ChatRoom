package frames;

import core.Frame;
import core.IoArgs;

import java.io.IOException;

public abstract class AbsReceiveFrame extends Frame {
    volatile int bodyRemaining;

    public AbsReceiveFrame(byte[] head) {
        super(head);
        bodyRemaining = getBodyLength();
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        if (bodyRemaining == 0) {
            return true;
        }
        bodyRemaining -= consumeBody(ioArgs);
        return bodyRemaining == 0;
    }

    @Override
    public final Frame nextFrame() {
        return null;
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;

}
