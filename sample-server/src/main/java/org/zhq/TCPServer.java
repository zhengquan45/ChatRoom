package org.zhq;

import org.zhq.box.StringReceivePacket;
import org.zhq.core.Connector;
import org.zhq.core.ScheduleJob;
import org.zhq.handle.ConnectorHandler;
import org.zhq.handle.ConnectorCloseHandlerChain;
import org.zhq.handle.ConnectorStringPacketChain;
import org.zhq.impl.schedule.IdleTimeoutScheduleJob;
import lombok.extern.slf4j.Slf4j;
import org.zhq.utils.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdaptor {
    private final int port;
    private final File cachePath;
    private ServerAcceptor acceptor;
    private ServerSocketChannel server;
    private final List<ConnectorHandler> connectorHandlerList = new CopyOnWriteArrayList<>();
    private final ServerStatistics statistics = new ServerStatistics();
    private final Map<String, Group> groups = new HashMap<>();

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
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
            if (acceptor.awaitRunning()) {
                log.info("server {} ready", server.getLocalAddress().toString());
                return true;
            } else {
                log.error("server boot fail");
                return false;
            }
        } catch (IOException e) {
            log.info("create org.zhq.TCPServer fail. exception:{}", e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }


        synchronized (connectorHandlerList) {
            for (ConnectorHandler connectorHandler : connectorHandlerList) {
                connectorHandler.exit();
            }
            connectorHandlerList.clear();
        }

        CloseUtil.close(server);
    }

    public void broadcast(String msg) {
        msg = "System notification:" + msg;
        synchronized (connectorHandlerList) {
            for (ConnectorHandler connectorHandler : connectorHandlerList) {
                sendMessageToClient(connectorHandler, msg);
            }
        }
    }

    @Override
    public void onNewClientArrived(SocketChannel channel) {
        try {
            ConnectorHandler connectorHandler = new ConnectorHandler(channel, cachePath);
            log.info("client:" + connectorHandler.getClientInfo() + " arrived");
            connectorHandler.getStringPacketChain()
                    .appendLast(statistics.statisticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain());

            connectorHandler.getCloseHandlerChain()
                    .appendLast(new RemoveQueueOnConnectorCloseChain());

            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(20 * 1000, connectorHandler);
            connectorHandler.schedule(scheduleJob);

            synchronized (connectorHandlerList) {
                connectorHandlerList.add(connectorHandler);
                log.info("client size:" + connectorHandlerList.size());
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
                "client size：" + connectorHandlerList.size(),
                "send size：" + statistics.sendSize,
                "receive size：" + statistics.receiveSize
        };
    }

    @Override
    public void sendMessageToClient(ConnectorHandler connectorHandler, String msg) {
        connectorHandler.send(msg);
        statistics.sendSize++;
    }

    private class RemoveQueueOnConnectorCloseChain extends ConnectorCloseHandlerChain {

        @Override
        protected boolean consume(ConnectorHandler connectorHandler, Connector connector) {
            synchronized (connectorHandlerList) {
                connectorHandlerList.remove(connectorHandler);
                Collection<Group> values = groups.values();
                for (Group group : values) {
                    group.removeMember(connectorHandler);
                }
            }
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler connectorHandler, StringReceivePacket stringReceivePacket) {
            String msg = stringReceivePacket.entity();

            if (msg.startsWith(Foo.COMMAND_GROUP_JOIN)) {
                String groupName = msg.substring(Foo.COMMAND_GROUP_JOIN.length() + 1);
                Group group = groups.get(groupName);
                if (group == null) {
                    group = new Group(groupName, TCPServer.this);
                }
                if (group.addMember(connectorHandler)) {
                    sendMessageToClient(connectorHandler, "join group:" + groupName);
                }
                return true;
            } else if (msg.startsWith(Foo.COMMAND_GROUP_LEAVE)) {
                String groupName = msg.substring(Foo.COMMAND_GROUP_LEAVE.length() + 1);
                Group group = groups.get(groupName);
                if (group == null) {
                    return true;
                }
                if (group.removeMember(connectorHandler)) {
                    sendMessageToClient(connectorHandler, "leave group:" + groupName);
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ConnectorHandler connectorHandler, StringReceivePacket stringReceivePacket) {
            sendMessageToClient(connectorHandler, stringReceivePacket.entity());
            return true;
        }
    }


}
