package org.zhq;

import org.zhq.core.Connector;
import org.zhq.core.IoContext;
import org.zhq.handle.ConnectorCloseHandlerChain;
import org.zhq.handle.ConnectorHandler;
import org.zhq.impl.IoSelectorProvider;
import lombok.extern.slf4j.Slf4j;
import org.zhq.impl.SchedulerImpl;
import org.zhq.utils.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClientTest {
    public static volatile boolean done = false;

    public static final int CLIENT_NUM = 2000;

    public static final int SEND_SPACE = 400;

    public static final int THREAD_SIZE = 4;

    public static final int CONNECT_SPACE = 0;

    public static void main(String[] args) throws IOException {
        ServerInfo serverInfo = UDPSearcher.searchServer(10000);
        log.info("server:{}", serverInfo.toString());
        if (serverInfo == null) {
            return;
        }

        File cachePath = Foo.getCacheDir("client/test");
        IoContext ioContext = IoContext.setUp()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();

        System.in.read();
        int size = 0;
        final List<TCPClient> tcpClientList = new ArrayList<>(CLIENT_NUM);

        final ConnectorCloseHandlerChain closeHandlerChain = new ConnectorCloseHandlerChain() {
            @Override
            protected boolean consume(ConnectorHandler connectorHandler, Connector connector) {
                tcpClientList.remove(connectorHandler);
                if (tcpClientList.size() == 0) {
                    CloseUtil.close(System.in);
                }
                return false;
            }
        };

        for (int i = 0; i < CLIENT_NUM; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(serverInfo, cachePath, false);
                if (tcpClient == null) {
                    throw new RuntimeException();
                }
                size++;
                tcpClient.getCloseHandlerChain().appendLast(closeHandlerChain);
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

        Runnable runnable = () -> {
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
        };

        List<Thread> threads = new ArrayList<>(THREAD_SIZE);
        for (int i = 0; i < THREAD_SIZE; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
        }

        System.in.read();

        done = true;
        for (TCPClient tcpClient : tcpClientList) {
            tcpClient.exit();
        }
        ioContext.close();

        for (Thread thread : threads) {
            thread.interrupt();
        }
    }
}
