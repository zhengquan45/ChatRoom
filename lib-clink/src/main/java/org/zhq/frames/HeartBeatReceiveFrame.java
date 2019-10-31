package org.zhq.frames;

import org.zhq.core.IoArgs;

public class HeartBeatReceiveFrame extends AbsReceiveFrame {
    static final HeartBeatReceiveFrame INSTANCE = new HeartBeatReceiveFrame();

    private  HeartBeatReceiveFrame() {
        super(HeartBeatSendFrame.HEARTBEAT);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) {
        return 0;
    }
}
