import core.Connector;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.*;
import java.nio.channels.SocketChannel;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class ClientHandler extends Connector{
    private final ClientHandlerCallBack callBack;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack callBack) throws IOException {
        this.callBack = callBack;
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
    protected void onReceiveNewMessage(String msg) {
        super.onReceiveNewMessage(msg);
        callBack.onNewMessageArrived(this,msg);
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
