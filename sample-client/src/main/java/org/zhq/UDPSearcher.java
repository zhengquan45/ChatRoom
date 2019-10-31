package org.zhq;

import org.zhq.constants.UDPConstants;
import lombok.extern.slf4j.Slf4j;
import org.zhq.utils.ByteUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author zhengquan
 */
@Slf4j
public class UDPSearcher {

    public static ServerInfo searchServer(int timeout){
        log.info("org.zhq.UDPSearcher start");
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listen(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (listener == null) {
            return null;
        }
        List<ServerInfo> serverInfoList = listener.getServerAndClose();
        if (serverInfoList.size() > 0) {
            return serverInfoList.get(0);
        }
        return null;

    }


    private static void sendBroadcast() throws IOException {
        log.info("org.zhq.UDPSearcher sendBroadcast started");
        DatagramSocket ds = new DatagramSocket();
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        byteBuffer.put(UDPConstants.HEADER);
        byteBuffer.putShort((short) 1);
        byteBuffer.putInt(UDPConstants.PORT_CLIENT_RESPONSE);
        int len = byteBuffer.position();
        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(UDPConstants.PORT_SERVER);
        log.info("org.zhq.UDPSearcher sendBroadcast ip:{} port:{} length:{} data:{}","255.255.255.255",UDPConstants.PORT_SERVER,len,byteBuffer.array());
        ds.send(requestPacket);
        ds.close();
        log.info("org.zhq.UDPSearcher sendBroadcast finished");
    }


    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        log.info("org.zhq.UDPSearcher listen start");
        CountDownLatch startLatch = new CountDownLatch(1);
        Listener listener = new Listener(UDPConstants.PORT_CLIENT_RESPONSE, receiveLatch, startLatch);
        listener.start();
        startLatch.await();
        return listener;
    }

    private static class Listener extends Thread {
        private final int port;
        private final CountDownLatch receiveLatch;
        private final CountDownLatch startLatch;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        private final byte[] buf = new byte[128];
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;
        private boolean done;
        private DatagramSocket ds = null;

        private Listener(int port, CountDownLatch receiveLatch, CountDownLatch startLatch) {
            this.port = port;
            this.receiveLatch = receiveLatch;
            this.startLatch = startLatch;
        }

        @Override
        public void run() {
            super.run();

            //通知监听已开始
            startLatch.countDown();

            try {
                ds = new DatagramSocket(port);
                DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);


                while (!done) {
                    ds.receive(receivePacket);
                    InetAddress ip = receivePacket.getAddress();
                    int port = receivePacket.getPort();
                    int dataLen = receivePacket.getLength();
                    byte[] data = receivePacket.getData();
                    boolean isValid = dataLen >= minLen && ByteUtil.startsWith(data, UDPConstants.HEADER);
                    log.info("org.zhq.UDPSearcher receive from ip:{} port:{} data:{} dataLen:{} isValid:{}", ip.getHostAddress(), port, data, dataLen, isValid);
                    if (!isValid) {
                        continue;
                    }
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buf, UDPConstants.HEADER.length, dataLen);
                    short cmd = byteBuffer.getShort();
                    int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort <= 0) {
                        log.info("org.zhq.UDPSearcher nonsupport cmd. cmd:{} serverPort:{}", cmd, serverPort);
                        continue;
                    }
                    String sn = new String(buf, minLen, dataLen - minLen);
                    ServerInfo serverInfo = new ServerInfo(ip.getHostAddress(), serverPort, sn);
                    serverInfoList.add(serverInfo);
                    receiveLatch.countDown();
                }
            } catch (Exception ignored) {
            } finally {
                close();
            }
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        List<ServerInfo> getServerAndClose() {
            done = true;
            close();
            return serverInfoList;
        }
    }
}
