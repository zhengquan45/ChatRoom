package frames;

import core.IoArgs;

public class CancelReceiveFrame extends AbsReceiveFrame {

    public CancelReceiveFrame(byte[] head) {
        super(head);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        return 0;
    }
}
