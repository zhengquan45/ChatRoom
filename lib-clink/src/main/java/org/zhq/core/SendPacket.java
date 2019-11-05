package org.zhq.core;


import java.io.IOException;
import java.io.InputStream;

public abstract class SendPacket<T extends InputStream> extends Packet<T> {

    public boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        canceled = true;
    }

    public int available() {
        InputStream inputStream = open();
        try {
            int available = inputStream.available();
            if (available < 0) {
                return 0;
            }
            return available;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
