package org.zhq.handle;

import org.zhq.box.StringReceivePacket;

public class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {
    @Override
    protected boolean consume(ConnectorHandler connectorHandler, StringReceivePacket stringReceivePacket) {
        return false;
    }
}
