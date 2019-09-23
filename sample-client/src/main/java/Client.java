import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


/**
 * @author zhengquan
 */
@Slf4j
public class Client {

    public static void main(String[] args) {
        ServerInfo serverInfo = UDPSearcher.searchServer(10000);
        log.info("server:{}", serverInfo.toString());
        if (serverInfo != null) {
            try {
                TCPClient.linkWith(serverInfo);
            } catch (IOException e) {
                log.info("client connect fail. exception:{}", e.getMessage());
            }
        }
    }
}
