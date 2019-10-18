package core;


import java.io.InputStream;

public abstract class SendPacket<T extends InputStream> extends Packet<T> {

    public boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }


    public void cancel(){
        canceled = true;
    }
}
