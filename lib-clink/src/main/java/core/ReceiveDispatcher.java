package core;

import java.io.Closeable;

public interface ReceiveDispatcher extends Closeable {
    void start();

    void stop();

    interface ReceivePacketCallBack{
        void onReceivePacketCompleted(ReceivePacket receivePacket);
    }
}
