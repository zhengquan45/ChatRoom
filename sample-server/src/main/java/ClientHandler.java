import box.StringReceivePacket;
import core.Connector;
import core.Packet;
import core.ReceivePacket;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class ClientHandler extends Connector {
    private final String clientInfo;
    private final File cachePath;
    private final ConnectorCloseHandlerChain closeHandlerChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();
    private final ExecutorService deliverThreadPool;
    public ClientHandler(SocketChannel socketChannel, File cachePath, ExecutorService deliverThreadPool) throws IOException {
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;
        this.deliverThreadPool = deliverThreadPool;
        setup(socketChannel);
    }

    public void exit() {
        CloseUtil.close(this);
        closeHandlerChain.handle(this, this);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeHandlerChain.handle(this, this);
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedNewPacket(ReceivePacket packet) {
        super.onReceivedNewPacket(packet);
        if (Packet.TYPE_MEMORY_STRING == packet.type()) {
            deliverStringPacket((StringReceivePacket) packet);
        }
    }

    private void deliverStringPacket(StringReceivePacket packet) {
        deliverThreadPool.execute(()-> stringPacketChain.handle(this, packet));
    }

    public ConnectorCloseHandlerChain getCloseHandlerChain() {
        return closeHandlerChain;
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }
}
