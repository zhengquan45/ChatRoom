package org.zhq.frames;

import org.zhq.core.Frame;
import org.zhq.core.IoArgs;

import java.io.IOException;

public class HeartBeatSendFrame extends AbsSendFrame {
    static final byte[] HEARTBEAT = new byte[]{0, 0, Frame.TYPE_COMMAND_HEART_BEAT, 0, 0, 0};

    public HeartBeatSendFrame() {
        super(HEARTBEAT);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
