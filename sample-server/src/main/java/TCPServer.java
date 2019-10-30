import box.StringReceivePacket;
import core.Connector;
import core.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdaptor {
    private final int port;
    private final File cachePath;
    private ServerAcceptor acceptor;
    private Selector selector;
    private ServerSocketChannel server;
    private final List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService deliveryPool;
    private final ServerStatistics statistics = new ServerStatistics();
    private final Map<String, Group> groups = new HashMap<>();

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        deliveryPool = Executors.newSingleThreadExecutor(new NamedThreadFactory("forward-Io"));
    }

    public boolean start() {
        try {
            acceptor = new ServerAcceptor(this);
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            server.register(acceptor.getSelector(), SelectionKey.OP_ACCEPT);
            log.info("server info : {}", server.getLocalAddress().toString());
            acceptor.start();
            if(acceptor.awaitRunning()){
                log.info("server {} ready",server.getLocalAddress().toString());
                return true;
            }else{
                log.error("server boot fail");
                return false;
            }
        } catch (IOException e) {
            log.info("create TCPServer fail. exception:{}", e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }


        synchronized (clientHandlerList) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }

        CloseUtil.close(server, selector);
        deliveryPool.shutdown();
    }

    public void broadcast(String msg) {
        msg = "System notification:" + msg;
        synchronized (clientHandlerList) {
            for (ClientHandler clientHandler : clientHandlerList) {
                sendMessageToClient(clientHandler, msg);
            }
        }
    }

    @Override
    public void onNewClientArrived(SocketChannel channel) {
        try {
            ClientHandler clientHandler = new ClientHandler(channel, cachePath, deliveryPool);
            log.info("client:" + clientHandler.getClientInfo() + " arrived");
            clientHandler.getStringPacketChain()
                    .appendLast(statistics.statisticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain());

            clientHandler.getCloseHandlerChain()
                    .appendLast(new RemoveQueueOnConnectorCloseChain());
            synchronized (clientHandlerList) {
                clientHandlerList.add(clientHandler);
                log.info("client size:" + clientHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("client connect is abnormally");
        }
    }

    /**
     * 获取当前的状态信息
     */
    Object[] getStatusString() {
        return new String[]{
                "client size：" + clientHandlerList.size(),
                "send size：" + statistics.sendSize,
                "receive size：" + statistics.receiveSize
        };
    }

    @Override
    public void sendMessageToClient(ClientHandler clientHandler, String msg) {
        clientHandler.send(msg);
        statistics.sendSize++;
    }

    private class RemoveQueueOnConnectorCloseChain extends ConnectorCloseHandlerChain {

        @Override
        protected boolean consume(ClientHandler clientHandler, Connector connector) {
            synchronized (clientHandlerList) {
                clientHandlerList.remove(clientHandler);
            }
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ClientHandler clientHandler, StringReceivePacket stringReceivePacket) {
            String msg = stringReceivePacket.entity();

            if (msg.startsWith(Foo.COMMAND_GROUP_JOIN)) {
                String groupName = msg.substring(Foo.COMMAND_GROUP_JOIN.length()+1);
                Group group = groups.get(groupName);
                if (group == null) {
                    group = new Group(groupName, TCPServer.this);
                }
                if (group.addMember(clientHandler)) {
                    sendMessageToClient(clientHandler, "join group:" + groupName);
                }
                return true;
            } else if (msg.startsWith(Foo.COMMAND_GROUP_LEAVE)) {
                String groupName = msg.substring(Foo.COMMAND_GROUP_LEAVE.length()+1);
                Group group = groups.get(groupName);
                if (group == null) {
                    return true;
                }
                if (group.removeMember(clientHandler)) {
                    sendMessageToClient(clientHandler, "leave group:" + groupName);
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ClientHandler clientHandler, StringReceivePacket stringReceivePacket) {
            sendMessageToClient(clientHandler, stringReceivePacket.entity());
            return true;
        }
    }


}
