
import box.StringReceivePacket;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class Group {
    private String name;
    private final GroupMessageAdaptor adaptor;
    private final Set<ClientHandler> members = new CopyOnWriteArraySet<>();

    public Group(String name, GroupMessageAdaptor adaptor) {
        this.name = name;
        this.adaptor = adaptor;
    }

    public String getName() {
        return name;
    }

    boolean addMember(ClientHandler clientHandler) {
        if (members.add(clientHandler)) {
            clientHandler.getStringPacketChain().appendLast(new ForwardConnectorStringPacketChain());
            log.info("group [{}] add new member:{}",name,clientHandler.getClientInfo());
            return true;
        }
        return false;
    }

    boolean removeMember(ClientHandler clientHandler) {
        if (members.remove(clientHandler)) {
            clientHandler.getStringPacketChain().remove(ForwardConnectorStringPacketChain.class);
            log.info("group [{}] leave a member:{}",name,clientHandler.getClientInfo());
            return true;
        }
        return false;
    }

    private class ForwardConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ClientHandler clientHandler, StringReceivePacket stringReceivePacket) {
            for (ClientHandler member : members) {
                if (member == clientHandler) {
                    continue;
                }
                adaptor.sendMessageToClient(clientHandler, stringReceivePacket.entity());
            }
            return true;
        }
    }

    interface GroupMessageAdaptor {
        void sendMessageToClient(ClientHandler clientHandler, String msg);
    }
}
