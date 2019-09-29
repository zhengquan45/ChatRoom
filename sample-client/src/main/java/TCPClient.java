import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;



/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class TCPClient {
    private final Socket socket;
    private final ClientReadHandler clientReadHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ClientReadHandler clientReadHandler) throws IOException {
        this.socket = socket;
        this.clientReadHandler = clientReadHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit(){
        clientReadHandler.exit();
        CloseUtil.close(printStream,socket);
    }

    public static TCPClient startWith(ServerInfo serverInfo) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(3000);
        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getIp()),serverInfo.getPort()),3000);
        log.info("connected server.client info [ip:{},port:{}] server info [ip:{},port:{}]",socket.getLocalAddress(),socket.getLocalPort(),socket.getInetAddress(),socket.getPort());


        try {
            ClientReadHandler clientReadHandler = new ClientReadHandler(socket);
            clientReadHandler.start();
            return new TCPClient(socket,clientReadHandler);
        }catch (Exception e){
            log.info("connect exception");
            CloseUtil.close(socket);
        }
        log.info("client quit");
        return null;
    }

    public void send(String msg) {
        printStream.println(msg);
    }

    static class ClientReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(Socket socket) throws IOException {
            this.inputStream = socket.getInputStream();
        }

        @Override
        public void run() {
            super.run();

            try {
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                while (!done) {
                    String msg;
                    try {
                       msg = socketInput.readLine();
                    }catch (SocketTimeoutException ignore){
                        continue;
                    }
                    //exception or timeout
                    if (msg == null) {
                        log.warn("connection closed. can't receive data");
                        break;
                    }
                    log.info("receive data:{}", msg);
                }
            } catch (Exception e) {
                if (!done) {
                    log.info("unexpected disconnection. exception:{}", e.getMessage());
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
}
