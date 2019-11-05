package org.zhq.handle;

import org.zhq.box.StringReceivePacket;
import org.zhq.core.Connector;
import org.zhq.core.IoContext;
import org.zhq.core.Packet;
import org.zhq.core.ReceivePacket;
import lombok.extern.slf4j.Slf4j;
import org.zhq.Foo;
import org.zhq.utils.CloseUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class ConnectorHandler extends Connector {
    private final String clientInfo;
    private final File cachePath;
    private final ConnectorCloseHandlerChain closeHandlerChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();
    public ConnectorHandler(SocketChannel socketChannel, File cachePath) throws IOException {
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;
        setup(socketChannel);
    }

    public void exit() {
        CloseUtil.close(this);
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
    protected OutputStream createNewReceiveDirectOutputStream(long length, byte[] headerInfo) {
        return new ByteArrayOutputStream();
    }

    @Override
    protected File createNewReceiveFile(long length, byte[] headerInfo) {
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
        IoContext.get().getScheduler().delivery(()-> stringPacketChain.handle(this, packet));
    }

    public ConnectorCloseHandlerChain getCloseHandlerChain() {
        return closeHandlerChain;
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }
}
