import core.Connector;
import core.Packet;
import core.ReceivePacket;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;


/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPClient extends Connector {

    public final File cachePath;

    public TCPClient(SocketChannel channel, File cachePath) throws IOException {
        this.cachePath = cachePath;
        setup(channel);
    }

    public void exit() {
        CloseUtil.close(this);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        log.error("connection closed");
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedNewPacket(ReceivePacket packet) {
        super.onReceivedNewPacket(packet);
        if (Packet.TYPE_MEMORY_STRING == packet.type()) {
            String string = (String) packet.entity();
            log.info("{}:{}", getKey(), string);
        }
    }

    public static TCPClient startWith(ServerInfo serverInfo, File cachePath) throws IOException {
        SocketChannel channel = SocketChannel.open();

        channel.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getIp()), serverInfo.getPort()));
        log.info("connected server.client info:{} server info :{}", channel.getLocalAddress(), channel.getRemoteAddress());


        try {
            return new TCPClient(channel, cachePath);
        } catch (Exception e) {
            log.info("connect exception");
            CloseUtil.close(channel);
        }
        log.info("client quit");
        return null;
    }
}
