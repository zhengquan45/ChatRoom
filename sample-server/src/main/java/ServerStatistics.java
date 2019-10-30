import box.StringReceivePacket;

public class ServerStatistics {
    long sendSize;
    long receiveSize;

    ConnectorStringPacketChain statisticsChain(){
        return new StatisticsConnectorStringPacketChain();
    }

    class StatisticsConnectorStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ClientHandler clientHandler, StringReceivePacket stringReceivePacket) {
            receiveSize++;
            return false;
        }
    }
}
