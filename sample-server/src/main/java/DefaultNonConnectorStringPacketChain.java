import box.StringReceivePacket;

public class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {
    @Override
    protected boolean consume(ClientHandler clientHandler, StringReceivePacket stringReceivePacket) {
        return false;
    }
}
