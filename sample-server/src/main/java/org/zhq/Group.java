package org.zhq;

import org.zhq.box.StringReceivePacket;
import org.zhq.handle.ConnectorHandler;
import org.zhq.handle.ConnectorStringPacketChain;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class Group {
    private String name;
    private final GroupMessageAdaptor adaptor;
    private final Set<ConnectorHandler> members = new CopyOnWriteArraySet<>();

    public Group(String name, GroupMessageAdaptor adaptor) {
        this.name = name;
        this.adaptor = adaptor;
    }

    public String getName() {
        return name;
    }

    boolean addMember(ConnectorHandler connectorHandler) {
        if (members.add(connectorHandler)) {
            connectorHandler.getStringPacketChain().appendLast(new ForwardConnectorStringPacketChain());
            log.info("group [{}] add new member:{}",name, connectorHandler.getClientInfo());
            return true;
        }
        return false;
    }

    boolean removeMember(ConnectorHandler connectorHandler) {
        if (members.remove(connectorHandler)) {
            connectorHandler.getStringPacketChain().remove(ForwardConnectorStringPacketChain.class);
            log.info("group [{}] leave a member:{}",name, connectorHandler.getClientInfo());
            return true;
        }
        return false;
    }

    private class ForwardConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler connectorHandler, StringReceivePacket stringReceivePacket) {
            for (ConnectorHandler member : members) {
                if (member == connectorHandler) {
                    continue;
                }
                adaptor.sendMessageToClient(connectorHandler, stringReceivePacket.entity());
            }
            return true;
        }
    }

    interface GroupMessageAdaptor {
        void sendMessageToClient(ConnectorHandler connectorHandler, String msg);
    }
}
