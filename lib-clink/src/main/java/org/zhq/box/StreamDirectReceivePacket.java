package org.zhq.box;

import org.zhq.core.Packet;
import org.zhq.core.ReceivePacket;

import java.io.OutputStream;

public class StreamDirectReceivePacket extends ReceivePacket<OutputStream,OutputStream> {

    private OutputStream outputStream;
    public StreamDirectReceivePacket(OutputStream outputStream,long len) {
        super(len);
        this.outputStream = outputStream;
    }

    @Override
    protected OutputStream buildEntity(OutputStream stream) {
        return outputStream;
    }

    @Override
    protected OutputStream createStream() {
        return outputStream;
    }

    @Override
    public byte type() {
        return Packet.TYPE_MEMORY_DIRECT;
    }
}
