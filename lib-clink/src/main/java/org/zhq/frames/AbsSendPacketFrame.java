package org.zhq.frames;

import org.zhq.core.Frame;
import org.zhq.core.IoArgs;
import org.zhq.core.SendPacket;

import java.io.IOException;

public abstract class AbsSendPacketFrame extends AbsSendFrame {
    protected volatile SendPacket<?> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    public synchronized SendPacket getPacket(){
        return packet;
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        if (packet == null && isSending()) {
            return true;
        }
        return super.handle(ioArgs);
    }

    protected abstract Frame buildNextFrame();

    public final synchronized boolean abort() {
        boolean sending = isSending();
        if (sending) {
            fillDirtyDataOnAbort();
        }
        packet = null;
        return !sending;
    }

    protected void fillDirtyDataOnAbort() {

    }
}
