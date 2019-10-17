package core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    void setSendProcessor(IoArgs.IoArgsEventProcessor processor);
    boolean postSendAsync() throws IOException;
}
