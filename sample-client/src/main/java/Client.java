import core.IoContext;
import impl.IoSelectorProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.*;


/**
 * @author zhengquan
 */
@Slf4j
public class Client {

    public static void main(String[] args) throws IOException {
        IoContext ioContext = IoContext.setUp().ioProvider(new IoSelectorProvider()).start();
        ServerInfo serverInfo = UDPSearcher.searchServer(10000);
        log.info("server:{}", serverInfo.toString());
        TCPClient tcpClient = null;
        try {
            tcpClient = TCPClient.startWith(serverInfo);
            if(tcpClient!=null){
                write(tcpClient);
            }
        } catch (IOException e) {
            log.info("client connect fail. exception:{}", e.getMessage());
        }finally {
            if(tcpClient!=null){
                tcpClient.exit();
            }
        }
        ioContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));


        while(true){
            String msg = input.readLine();
            tcpClient.send(msg);

            if(TCPConstants.END.equalsIgnoreCase(msg)){
                break;
            }
        }
    }
}
