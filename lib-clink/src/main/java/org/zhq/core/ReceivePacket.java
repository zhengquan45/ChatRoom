package org.zhq.core;


import java.io.IOException;
import java.io.OutputStream;

public abstract class ReceivePacket<Stream extends OutputStream, Entity> extends Packet<Stream> {

    private Entity entity;

    public ReceivePacket(long len) {
        this.length = len;
    }

    public Entity entity(){
        return entity;
    }

    protected abstract Entity buildEntity(Stream stream);

    @Override
    protected void closeStream() throws IOException {
        super.closeStream();
        entity = buildEntity(stream);
    }
}
