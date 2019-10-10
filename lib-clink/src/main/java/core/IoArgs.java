package core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IoArgs {
    private ByteBuffer buffer = ByteBuffer.allocate(256);

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    public String buffer2String(){
        return new String(buffer.array(),0,buffer.position()-1);
    }

    public interface  IoArgsEventListener{
        void onStarted(IoArgs args);
        void onCompleted(IoArgs args);
    }
}
