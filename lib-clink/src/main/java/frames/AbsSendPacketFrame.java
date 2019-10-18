package frames;

import core.SendPacket;

public abstract class AbsSendPacketFrame extends AbsSendFrame {
    protected SendPacket<?> packet;
    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier,SendPacket packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }
}
