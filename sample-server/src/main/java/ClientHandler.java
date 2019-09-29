import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class ClientHandler{
    private final Socket socket;
    private final ClientReadHandler clientReadHandler;
    private final ClientWriteHandler clientWriteHandler;
    private final ClientHandlerCallBack callBack;
    private final String clientInfo;

    public ClientHandler(Socket socket, ClientHandlerCallBack callBack) throws IOException {
        this.socket = socket;
        this.clientReadHandler = new ClientReadHandler(socket.getInputStream());
        this.clientWriteHandler = new ClientWriteHandler(socket.getOutputStream());
        this.callBack = callBack;
        this.clientInfo = MessageFormat.format("[ip:%s,port:%d]", socket.getInetAddress(), socket.getPort());
        log.info("new client connection. {}",clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void send(String msg) {
        clientWriteHandler.send(msg);
    }

    public void exit() {
        clientReadHandler.exit();
        clientWriteHandler.exit();
        CloseUtil.close(socket);
        log.info("client quit. ip:{},port:{}", socket.getInetAddress(), socket.getPort());
    }

    public void readToPrint() {
        clientReadHandler.start();
        clientWriteHandler.start();
    }

    private void exitBySelf() {
        exit();
        callBack.onSelfClosed(this);
    }

    public interface ClientHandlerCallBack {
        /**
         * 关闭自己通知外部的回调
         * @param clientHandler
         */
        void onSelfClosed(ClientHandler clientHandler);

        /**
         * 新信息到达的回调
         * @param clientHandler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler clientHandler,String msg);
    }

    class ClientReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            try {
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                while (!done) {
                    String msg = socketInput.readLine();
                    //exception or timeout
                    if (msg == null) {
                        log.warn("client can't read data");
                        ClientHandler.this.exitBySelf();
                        break;
                    }
                    callBack.onNewMessageArrived(ClientHandler.this,msg);
                }
            } catch (IOException e) {
                if (!done) {
                    log.info("unexpected disconnection from client. exception:{}", e.getMessage());
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                CloseUtil.close(inputStream);
            }

        }

        void exit() {
            done = true;
            CloseUtil.close(inputStream);
        }
    }


    class ClientWriteHandler extends Thread {

        private boolean done = false;
        private final OutputStream outputStream;
        private final LinkedBlockingQueue<String> queue;

        ClientWriteHandler(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.queue = new LinkedBlockingQueue<>();
        }

        @Override
        public void run() {
            super.run();
            PrintStream socketOutput = new PrintStream(outputStream);
            while (!done) {
                String msg = null;
                try {
                    msg = queue.take();
                } catch (InterruptedException ignore) {}
                if (msg != null) {
                    try {
                        socketOutput.println(msg);
                    } catch (Exception ignored) {}
                }

            }
        }

        void exit() {
            done = true;
            CloseUtil.close(outputStream);
            queue.clear();
        }

        void send(String msg) {
            queue.add(msg);
        }

    }
}
