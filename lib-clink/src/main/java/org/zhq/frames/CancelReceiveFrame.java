package org.zhq.frames;

import org.zhq.core.IoArgs;

public class CancelReceiveFrame extends AbsReceiveFrame {

    public CancelReceiveFrame(byte[] head) {
        super(head);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        return 0;
    }
}
