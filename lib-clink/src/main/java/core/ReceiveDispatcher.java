package core;

import java.io.Closeable;

public interface ReceiveDispatcher extends Closeable {
    void start();

    void stop();

    interface ReceivePacketCallBack{
        ReceivePacket<?,?> onArrivedNewPacket(byte type,long length);
        void onReceivePacketCompleted(ReceivePacket receivePacket);
    }
}
