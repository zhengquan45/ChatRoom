package org.zhq;

import org.zhq.box.StringReceivePacket;
import org.zhq.core.Connector;
import org.zhq.core.Packet;
import org.zhq.core.ReceivePacket;
import lombok.extern.slf4j.Slf4j;
import org.zhq.handle.ConnectorHandler;
import org.zhq.handle.ConnectorStringPacketChain;
import org.zhq.utils.CloseUtil;

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
public class TCPClient extends ConnectorHandler {

    public TCPClient(SocketChannel channel, File cachePath, boolean printReceiveString) throws IOException {
        super(channel, cachePath);
        if (printReceiveString) {
            getStringPacketChain().appendLast(new PrintReceiveStringPacketChain());
        }
    }

    public static TCPClient startWith(ServerInfo serverInfo, File cachePath)throws IOException{
       return startWith(serverInfo,cachePath,true);
    }

    public static TCPClient startWith(ServerInfo serverInfo, File cachePath,boolean printReceiveString) throws IOException {
        SocketChannel channel = SocketChannel.open();

        channel.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getIp()), serverInfo.getPort()));
        log.info("connected server.client info:{} server info :{}", channel.getLocalAddress(), channel.getRemoteAddress());


        try {
            return new TCPClient(channel, cachePath,printReceiveString);
        } catch (Exception e) {
            log.info("connect exception");
            CloseUtil.close(channel);
        }
        log.info("client quit");
        return null;
    }

    private class PrintReceiveStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler connectorHandler, StringReceivePacket stringReceivePacket) {
            String msg = stringReceivePacket.entity();
            log.info("receive msg:{}", msg);
            return true;
        }
    }
}
