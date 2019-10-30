import core.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPServer implements ClientHandler.ClientHandlerCallBack, ServerAcceptor.AcceptListener {
    private final int port;
    private final File cachePath;
    private ServerAcceptor acceptor;
    private Selector selector;
    private ServerSocketChannel server;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardThreadPoolExecutor;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        forwardThreadPoolExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("forward-Io"));
    }

    public boolean start() {
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            log.info("server info : {}", server.getLocalAddress().toString());
            acceptor = new ServerAcceptor(this);
            acceptor.start();
        } catch (IOException e) {
            log.info("create TCPServer fail. exception:{}", e.getMessage());
            return false;
        }
        return true;
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }

        CloseUtil.close(server, selector);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }

        forwardThreadPoolExecutor.shutdown();
    }

    public synchronized void broadcast(String msg) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(msg);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler clientHandler) {
        clientHandlerList.remove(clientHandler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler clientHandler, String msg) {
        forwardThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler handler : clientHandlerList) {
                    if (!handler.equals(clientHandler)) {
                        handler.send(msg);
                    }
                }
            }
        });
    }

    @Override
    public void onNewClientArrived(SocketChannel channel) {
        try {
            ClientHandler clientHandler = new ClientHandler(channel, this, cachePath);
            log.info("client:" + clientHandler.getClientInfo() + " arrived");
            synchronized (TCPServer.this) {
                clientHandlerList.add(clientHandler);
                log.info("client size:" + clientHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("client connect is abnormally");
        }
    }
}
