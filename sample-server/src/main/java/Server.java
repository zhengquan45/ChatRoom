import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * @author Administrator
 */
@Slf4j
public class Server {


    public static void main(String[] args) throws IOException {
        
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean succeed = tcpServer.start();
        if(!succeed){
            log.info("TCP connect fail");
            return;
        }
        UDPProvider.start(TCPConstants.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String msg;
        do {
            msg = bufferedReader.readLine();
            tcpServer.broadcast(msg);
        }while(!TCPConstants.END.equalsIgnoreCase(msg));

        UDPProvider.stop();
        tcpServer.stop();
    }
}
