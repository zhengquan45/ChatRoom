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
    public static void linkWith(ServerInfo serverInfo) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(3000);
        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getIp()),serverInfo.getPort()),3000);
        log.info("connected server.client info [ip:{},port:{}] server info [ip:{},port:{}]",socket.getLocalAddress(),socket.getLocalPort(),socket.getInetAddress(),socket.getPort());


        try {
            ClientReadHandler clientReadHandler = new ClientReadHandler(socket);
            clientReadHandler.start();
            write(socket);
            clientReadHandler.exit();
        }catch (Exception e){
            log.info("close exception");
        }
        socket.close();
        log.info("client quit");
    }

    private static void write(Socket socket) throws IOException {
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        PrintStream socketOutput = new PrintStream(socket.getOutputStream());

        while(true){
            String msg = input.readLine();
            socketOutput.println(msg);

            if(TCPConstants.END.equalsIgnoreCase(msg)){
                break;
            }
        }
        socketOutput.close();
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
