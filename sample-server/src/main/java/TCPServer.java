import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPServer implements ClientHandler.ClientHandlerCallBack{
    private final int port;
    private ClientListener listener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardThreadPoolExecutor;

    public TCPServer(int port) {
        this.port = port;
        forwardThreadPoolExecutor = Executors.newFixedThreadPool(5);
    }

    public boolean start(){
        try {
            listener = new ClientListener(port);
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
        forwardThreadPoolExecutor.shutdown();
        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }
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
        log.info("receive from {} data {}",clientHandler.getClientInfo(),msg);
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
        private ServerSocket server;
        private boolean done = false;
        private static final String CLIENT_LISTENER_NAME = "Thread-listen-client";

        public ClientListener(int port) throws IOException {
            super(CLIENT_LISTENER_NAME);
            server = new ServerSocket(port);
            log.info("server info. [ip:{} port:{}]",server.getInetAddress(),server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();
            log.info("server ready");
            while(!done){
                Socket socket;
                try {
                    socket = server.accept();
                    ClientHandler clientHandler = new ClientHandler(socket,TCPServer.this);
                    synchronized (TCPServer.this) {
                        clientHandlerList.add(clientHandler);
                    }
                    clientHandler.readToPrint();
                } catch (IOException e) {
                  log.info("accept client fail. exception:{}",e.getMessage());
                }
            }
            log.info("server closed");
        }

        void exit(){
            done = true;
            try {
                server.close();
            } catch (IOException e) {
                log.info("ServerSocket close fail. exception:{}",e.getMessage());
            }

        }
    }

}
