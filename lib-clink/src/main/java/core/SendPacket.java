package core;

public abstract class SendPacket extends Packet{
    public abstract byte[]bytes();
    public boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }
}
