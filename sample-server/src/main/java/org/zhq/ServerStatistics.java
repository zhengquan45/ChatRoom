package org.zhq;

import org.zhq.box.StringReceivePacket;
import org.zhq.handle.ConnectorHandler;
import org.zhq.handle.ConnectorStringPacketChain;

public class ServerStatistics {
    long sendSize;
    long receiveSize;

    ConnectorStringPacketChain statisticsChain(){
        return new StatisticsConnectorStringPacketChain();
    }

    class StatisticsConnectorStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler connectorHandler, StringReceivePacket stringReceivePacket) {
            receiveSize++;
            return false;
        }
    }
}
