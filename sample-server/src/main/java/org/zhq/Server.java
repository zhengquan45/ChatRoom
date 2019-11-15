package org.zhq;

import org.zhq.constants.TCPConstants;
import org.zhq.core.IoContext;
import org.zhq.impl.IoSelectorProvider;
import org.zhq.impl.IoStealingSelectorProvider;
import org.zhq.impl.SchedulerImpl;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * @author Administrator
 */
@Slf4j
public class Server {


    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("server");
        IoContext ioContext = IoContext.setUp()
                .ioProvider(new IoStealingSelectorProvider(1))
                .scheduler(new SchedulerImpl(1)).start();
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER, cachePath);
        boolean succeed = tcpServer.start();
        if (!succeed) {
            log.info("TCP connect fail");
            return;
        }
        UDPProvider.start(TCPConstants.PORT_SERVER);
        FooGui gui = new FooGui("Clink-org.zhq.Server", tcpServer::getStatusString);
        gui.doShow();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String msg;
        do {
            msg = bufferedReader.readLine();
            if (msg == null || TCPConstants.END.equalsIgnoreCase(msg)) {
                break;
            }
            if (msg.length() == 0) {
                continue;
            }
            tcpServer.broadcast(msg);
        } while (true);

        UDPProvider.stop();
        tcpServer.stop();
        gui.doDismiss();
        ioContext.close();
    }
}
