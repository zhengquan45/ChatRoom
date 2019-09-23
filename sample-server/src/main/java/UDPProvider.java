import lombok.extern.slf4j.Slf4j;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author zhengquan
 */
@Slf4j
public class UDPProvider {

    private static Provider PROVIDER_INSTANCE;


    static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {
        private final byte[] sn;
        private final int port;
        private boolean done;
        private DatagramSocket ds;
        final byte[] buf = new byte[512];

        public Provider(String sn, int port) {
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            log.info("UDPProvider provider start");
            try {
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (!done) {
                    ds.receive(packet);
                    InetAddress clientIp = packet.getAddress();
                    int clientPort = packet.getPort();
                    int clientLength = packet.getLength();
                    byte[] data = packet.getData();
                    boolean isValid = data.length >= (UDPConstants.HEADER.length + 2 + 4)
                            && ByteUtil.startsWith(data, UDPConstants.HEADER);

                    log.info("UDPProvider receive from ip:{} port:{} length:{} data:{},isValid:{}", clientIp.getHostAddress(), clientPort, clientLength, data, isValid);

                    if (!isValid) {
                        continue;
                    }
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) (data[index++] << 8 | (data[index++] & 0xff));
                    int responsePort = ((data[index++] << 24) |
                            ((data[index++] & 0xff) << 16) |
                            ((data[index++] & 0xff) << 8) |
                            (data[index++] & 0xff));
                    if (cmd == 1 && responsePort > 0) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short)2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        DatagramPacket responsePacket = new DatagramPacket(buf,len,clientIp,responsePort);
                        ds.send(responsePacket);
                        log.info("UDPProvider send to ip:{} port:{} length:{} data:{}",clientIp.getHostAddress(),responsePort,len,buf);
                    }else{
                        log.info("UDPProvider nonsupport cmd. cmd:{}",cmd);
                    }
                }
            } catch (Exception ignore) {
            } finally {
                close();
            }
            log.info("UDPProvider provider stop");
        }

        void exit() {
            this.done = true;
            close();
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }
    }
}
