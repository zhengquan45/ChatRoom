import core.NamedThreadFactory;
import impl.IoSelectorProvider;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPServer implements ClientHandler.ClientHandlerCallBack{
    private final int port;
    private final File cachePath;
    private ClientListener listener;
    private Selector selector;
    private ServerSocketChannel server;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardThreadPoolExecutor;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        forwardThreadPoolExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("forward-Io"));
    }

    public boolean start(){
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            log.info("server info : {}",server.getLocalAddress().toString());
            listener = new ClientListener();
            listener.start();
        } catch (IOException e) {
            log.info("create TCPServer fail. exception:{}",e.getMessage());
            return false;
        }
        return true;
    }

    public void stop(){
        if(listener!=null){
            listener.exit();
        }

        CloseUtil.close(server,selector);

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
        forwardThreadPoolExecutor.execute(()->{
            synchronized (TCPServer.this) {
                for (ClientHandler handler : clientHandlerList) {
                    if(!handler.equals(clientHandler)) {
                        handler.send(msg);
                    }
                }
            }
        });
    }

    private class ClientListener extends Thread{
        private boolean done = false;
        private static final String CLIENT_LISTENER_NAME = "Thread-listen-client";

        public ClientListener() throws IOException {
            super(CLIENT_LISTENER_NAME);

        }

        @Override
        public void run() {
            super.run();
            Selector selector = TCPServer.this.selector;
            log.info("server ready");
            while(!done){
                try {
                    if(selector.select()==0){
                        if(done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        if(done){
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if(key.isAcceptable()){
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            ClientHandler clientHandler = new ClientHandler(socketChannel,TCPServer.this, cachePath);
                            synchronized (TCPServer.this) {
                                clientHandlerList.add(clientHandler);
                            }
                        }
                    }
                } catch (IOException e) {
                  log.info("accept client fail. exception:{}",e.getMessage());
                }
            }
            log.info("server closed");
        }

        void exit(){
            done = true;
            selector.wakeup();
        }
    }

}
