package core;

import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector {
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;

    public void setup(SocketChannel socketChannel){
        this.channel = socketChannel;
    }
}
