import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ServerAcceptor extends Thread {

    private boolean done = false;
    private static final String CLIENT_LISTENER_NAME = "Thread-listen-client";
    private final AcceptListener acceptListener;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Selector selector;

    public ServerAcceptor(AcceptListener acceptListener) throws IOException {
        super(CLIENT_LISTENER_NAME);
        this.acceptListener = acceptListener;
        this.selector = Selector.open();
    }
    boolean awaitRunning(){
        try {
            latch.await();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        super.run();
        latch.countDown();
        Selector selector = this.selector;
        log.info("server ready");
        while (!done) {
            try {
                if (selector.select() == 0) {
                    if (done) {
                        break;
                    }
                    continue;
                }
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    if (done) {
                        break;
                    }
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        acceptListener.onNewClientArrived(socketChannel);
                    }
                }
            } catch (IOException e) {
                log.info("accept client fail. exception:{}", e.getMessage());
            }
        }
        log.info("server closed");

    }

    void exit(){
        done = true;
        CloseUtil.close(selector);
    }

    interface AcceptListener {
        void onNewClientArrived(SocketChannel channel);
    }
}
