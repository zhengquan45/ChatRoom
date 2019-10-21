package frames;

import core.Frame;
import core.IoArgs;

import java.io.IOException;

public class CancelSendFrame extends AbsSendFrame{

    public CancelSendFrame(short identifier) {
        super(0, Frame.TYPE_COMMAND_SEND_CANCEL, Frame.FLAG_NONE, identifier);
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
