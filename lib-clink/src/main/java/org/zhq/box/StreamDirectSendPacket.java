package org.zhq.box;

import org.zhq.core.Packet;
import org.zhq.core.SendPacket;

import java.io.InputStream;

public class StreamDirectSendPacket extends SendPacket<InputStream> {
    private InputStream inputStream;

    public StreamDirectSendPacket(InputStream inputStream) {
        this.inputStream = inputStream;
        this.length = MAX_PACKET_SIZE;
    }

    @Override
    protected InputStream createStream() {
        return inputStream;
    }

    @Override
    public byte type() {
        return Packet.TYPE_MEMORY_DIRECT;
    }
}
