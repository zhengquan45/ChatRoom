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

    private final Map<ConnectorHandler, ConnectorHandler> audioCmdToStreamMap = new HashMap<>(100);
    private final Map<ConnectorHandler, ConnectorHandler> audioStreamToCmdMap = new HashMap<>(100);

    private final Map<ConnectorHandler, AudioRoom> audioStreamRoomMap = new HashMap<>();
    private final Map<String, AudioRoom> audioRoomMap = new HashMap<>(50);

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
                    .appendLast(new ParseCommandConnectorStringPacketChain())
                    .appendLast(new ParseAudioStreamCommandStringPacketChain());

            connectorHandler.getCloseHandlerChain()
                    .appendLast(new RemoveAudioQueueOnConnectorClosedChain())
                    .appendLast(new RemoveQueueOnConnectorCloseChain());

            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(20 * 1000, connectorHandler);
            connectorHandler.schedule(scheduleJob);

            synchronized (connectorHandlerList) {
                connectorHandlerList.add(connectorHandler);
                log.info("client size:" + connectorHandlerList.size());
            }
            sendMessageToClient(connectorHandler, Foo.COMMAND_INFO_NAME + connectorHandler.getKey().toString());
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


    private class ParseAudioStreamCommandStringPacketChain extends org.zhq.handle.ConnectorHandlerChain<StringReceivePacket> {
        @Override
        protected boolean consume(ConnectorHandler connectorHandler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            if (str.startsWith(Foo.COMMAND_CONNECTOR_BIND)) {
                String key = str.substring(Foo.COMMAND_CONNECTOR_BIND.length());
                ConnectorHandler audioStreamConnector = findConnectorFromKey(key);
                if (audioStreamConnector != null) {
                    audioCmdToStreamMap.put(connectorHandler, audioStreamConnector);
                    audioStreamToCmdMap.put(audioStreamConnector, connectorHandler);

                    audioStreamConnector.changeToBridge();
                }
            } else if (str.startsWith(Foo.COMMAND_AUDIO_CREATE_ROOM)) {
                ConnectorHandler audioStreamConnector = findAudioStreamConnector(connectorHandler);
                if (audioStreamConnector != null) {
                    AudioRoom room = createNewRoom();
                    joinRoom(room, audioStreamConnector);
                    sendMessageToClient(connectorHandler, Foo.COMMAND_INFO_AUDIO_ROOM + room.getRoomCode());
                }
            } else if (str.startsWith(Foo.COMMAND_AUDIO_LEAVE_ROOM)) {
                ConnectorHandler audioStreamConnector = findAudioStreamConnector(connectorHandler);
                if (audioStreamConnector != null) {
                    dissolveRoom(audioStreamConnector);
                    sendMessageToClient(connectorHandler, Foo.COMMAND_INFO_AUDIO_STOP);
                }
            } else if (str.startsWith(Foo.COMMAND_AUDIO_JOIN_ROOM)) {
                ConnectorHandler audioStreamConnector = findAudioStreamConnector(connectorHandler);
                if (audioStreamConnector != null) {
                    String roomCode = str.substring(Foo.COMMAND_AUDIO_JOIN_ROOM.length());
                    AudioRoom audioRoom = audioRoomMap.get(roomCode);
                    if (audioRoom != null && joinRoom(audioRoom, audioStreamConnector)) {
                        ConnectorHandler theOtherHandler = audioRoom.getTheOtherHandler(audioStreamConnector);
                        theOtherHandler.bindToBridge(audioStreamConnector.getSender());
                        audioStreamConnector.bindToBridge(theOtherHandler.getSender());

                        sendMessageToClient(connectorHandler, Foo.COMMAND_INFO_AUDIO_START);
                        sendMessageToClient(theOtherHandler, Foo.COMMAND_INFO_AUDIO_START);
                    } else {
                        sendMessageToClient(connectorHandler, Foo.COMMAND_INFO_AUDIO_ERROR);
                    }
                }
            } else {
                return false;
            }
            return true;
        }
    }

    private void dissolveRoom(ConnectorHandler audioStreamConnector) {
        AudioRoom audioRoom = audioStreamRoomMap.get(audioStreamConnector);
        if (audioRoom == null) {
            return;
        }
        List<ConnectorHandler> connectors = audioRoom.getConnectors();
        for (ConnectorHandler connector : connectors) {
            connector.unBindToBridge();
            audioStreamRoomMap.remove(connector);
            if (connector != audioStreamConnector) {
                sendMessageToClient(connector, Foo.COMMAND_INFO_AUDIO_STOP);
            }
        }
        audioRoomMap.remove(audioRoom.getRoomCode());
    }

    private boolean joinRoom(AudioRoom room, ConnectorHandler audioStreamConnector) {
        if (room.enterRoom(audioStreamConnector)) {
            audioStreamRoomMap.put(audioStreamConnector, room);
            return true;
        }
        return false;
    }

    private AudioRoom createNewRoom() {
        AudioRoom audioRoom;
        do {
            audioRoom = new AudioRoom();
        } while (audioRoomMap.containsKey(audioRoom.getRoomCode()));
        audioRoomMap.put(audioRoom.getRoomCode(), audioRoom);
        return audioRoom;
    }

    private ConnectorHandler findAudioStreamConnector(ConnectorHandler connectorHandler) {
        return audioCmdToStreamMap.get(connectorHandler);
    }

    private ConnectorHandler findConnectorFromKey(String key) {
        for (ConnectorHandler connectorHandler : connectorHandlerList) {
            if (connectorHandler.getKey().toString().equalsIgnoreCase(key)) {
                return connectorHandler;
            }
        }
        return null;
    }

    private class RemoveAudioQueueOnConnectorClosedChain extends org.zhq.handle.ConnectorHandlerChain<Connector> {
        @Override
        protected boolean consume(ConnectorHandler connectorHandler, Connector connector) {

            if (audioCmdToStreamMap.containsKey(connectorHandler)) {
                audioCmdToStreamMap.remove(connectorHandler);
            } else if (audioStreamToCmdMap.containsKey(connectorHandler)) {
                audioStreamToCmdMap.remove(connectorHandler);
                dissolveRoom(connectorHandler);
            }
            return false;
        }
    }

}
