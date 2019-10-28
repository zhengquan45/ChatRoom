import core.IoContext;
import impl.IoSelectorProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClientTest {
    public static boolean done = false;

    public static final int CLIENT_NUM = 1000;

    public static final int SEND_SPACE = 100;

    public static final int CONNECT_SPACE = 0;

    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client/test");
        IoContext ioContext = IoContext.setUp().ioProvider(new IoSelectorProvider()).start();
        ServerInfo serverInfo = UDPSearcher.searchServer(10000);
        log.info("server:{}", serverInfo.toString());
        if (serverInfo == null) {
            return;
        }

        int size = 0;
        final List<TCPClient> tcpClientList = new ArrayList<>();
        for (int i = 0; i < CLIENT_NUM; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(serverInfo, cachePath);
                if (tcpClient == null) {
                    throw new RuntimeException();
                }
                size++;
                tcpClientList.add(tcpClient);
            } catch (Exception e) {
                log.error("connect exception:{}", e.getMessage());
            }
            if (CONNECT_SPACE > 0) {
                try {
                    Thread.sleep(CONNECT_SPACE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        log.info("{} connection ready", size);

        System.in.read();

        Thread thread = new Thread(() -> {
            while (!done) {
                for (TCPClient tcpClient : tcpClientList) {
                    tcpClient.send("Hello~~~");
                }
                try {
                    Thread.sleep(SEND_SPACE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        thread.start();

        System.in.read();

        done = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (TCPClient tcpClient : tcpClientList) {
            tcpClient.exit();
        }
        ioContext.close();

    }
}
