package org.zhq.handle;

import org.zhq.core.Connector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultPrintConnectorCloseChain extends ConnectorCloseHandlerChain {
    @Override
    protected boolean consume(ConnectorHandler connectorHandler, Connector connector) {
        log.info("connect[{}] key[{}] close.", connectorHandler.getClientInfo(), connectorHandler.getKey().toString());
        return true;
    }
}
