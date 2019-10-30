import core.Connector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultPrintConnectorCloseChain extends ConnectorCloseHandlerChain {
    @Override
    protected boolean consume(ClientHandler clientHandler, Connector connector) {
        log.info("connect[{}] key[{}] close.", clientHandler.getClientInfo(), clientHandler.getKey().toString());
        return false;
    }
}
