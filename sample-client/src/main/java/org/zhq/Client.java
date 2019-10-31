package org.zhq;

import org.zhq.constants.TCPConstants;
import org.zhq.box.FileSendPacket;
import org.zhq.core.Connector;
import org.zhq.core.IoContext;
import org.zhq.core.ScheduleJob;
import org.zhq.handle.ConnectorCloseHandlerChain;
import org.zhq.handle.ConnectorHandler;
import org.zhq.impl.IoSelectorProvider;
import lombok.extern.slf4j.Slf4j;
import org.zhq.impl.SchedulerImpl;
import org.zhq.impl.schedule.IdleTimeoutScheduleJob;
import org.zhq.utils.CloseUtil;

import java.io.*;


/**
 * @author zhengquan
 */
@Slf4j
public class Client {

    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client");
        IoContext ioContext = IoContext.setUp()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();
        ServerInfo serverInfo = UDPSearcher.searchServer(10000);
        log.info("server:{}", serverInfo.toString());
        TCPClient tcpClient = null;
        try {
            tcpClient = TCPClient.startWith(serverInfo, cachePath);
            tcpClient.getCloseHandlerChain().appendLast(new ConnectorCloseHandlerChain() {
                @Override
                protected boolean consume(ConnectorHandler connectorHandler, Connector connector) {
                    CloseUtil.close(System.in);
                    return true;
                }
            });
            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10*1000,tcpClient);
            tcpClient.schedule(scheduleJob);
            if (tcpClient != null) {
                write(tcpClient);
            }
        } catch (IOException e) {
            log.info("client connect fail. exception:{}", e.getMessage());
        } finally {
            if (tcpClient != null) {
                tcpClient.exit();
            }
        }
        ioContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));


        do {
            String msg = input.readLine();
            if (msg == null || TCPConstants.END.equalsIgnoreCase(msg)) {
                break;
            }
            if (msg.length() == 0) {
                continue;
            }
            if (msg.startsWith("--f")) {
                String[] array = msg.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket fileSendPacket = new FileSendPacket(file);
                        tcpClient.send(fileSendPacket);
                        continue;
                    }
                }
            }
            tcpClient.send(msg);
        }while (true);
    }
}
