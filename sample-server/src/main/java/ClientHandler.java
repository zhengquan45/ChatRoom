import core.Connector;
import core.Packet;
import core.ReceivePacket;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class ClientHandler extends Connector{
    private final ClientHandlerCallBack callBack;
    private final String clientInfo;
    public final File cachePath;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack callBack, File cachePath) throws IOException {
        this.callBack = callBack;
        this.cachePath = cachePath;
        setup(socketChannel);
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        log.info("new client connection. {}", clientInfo);
    }

    public void exit() {
        CloseUtil.close(this);
        log.info("client quit. {}", clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
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
            callBack.onNewMessageArrived(this,string);
        }
    }

    private void exitBySelf() {
        exit();
        callBack.onSelfClosed(this);
    }

    public interface ClientHandlerCallBack {
        /**
         * 关闭自己通知外部的回调
         *
         * @param clientHandler
         */
        void onSelfClosed(ClientHandler clientHandler);

        /**
         * 新信息到达的回调
         *
         * @param clientHandler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }


}
