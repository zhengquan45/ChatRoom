import core.Connector;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;


/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPClient extends Connector {

    public TCPClient(SocketChannel channel) throws IOException {
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


    public static TCPClient startWith(ServerInfo serverInfo) throws IOException {
        SocketChannel channel = SocketChannel.open();

        channel.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getIp()), serverInfo.getPort()));
        log.info("connected server.client info:{} server info :{}", channel.getLocalAddress(), channel.getRemoteAddress());


        try {
            return new TCPClient(channel);
        } catch (Exception e) {
            log.info("connect exception");
            CloseUtil.close(channel);
        }
        log.info("client quit");
        return null;
    }
}
