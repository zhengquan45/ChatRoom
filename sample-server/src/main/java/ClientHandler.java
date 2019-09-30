import lombok.extern.slf4j.Slf4j;
import utils.CloseUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
@Slf4j
public class ClientHandler {
    private final SocketChannel socketChannel;
    private final ClientReadHandler clientReadHandler;
    private final ClientWriteHandler clientWriteHandler;
    private final ClientHandlerCallBack callBack;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack callBack) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        Selector readSelector = Selector.open();
        this.socketChannel.register(readSelector, SelectionKey.OP_READ);
        Selector writeSelector = Selector.open();
        this.socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.clientReadHandler = new ClientReadHandler(readSelector);
        this.clientWriteHandler = new ClientWriteHandler(writeSelector);
        this.callBack = callBack;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        log.info("new client connection. {}", clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void send(String msg) {
        clientWriteHandler.send(msg);
    }

    public void exit() {
        clientReadHandler.exit();
        clientWriteHandler.exit();
        CloseUtil.close(socketChannel);
        log.info("client quit. {}", clientInfo);
    }

    public void readToPrint() {
        clientReadHandler.start();
        clientWriteHandler.start();
    }

    private void exitBySelf() {
        exit();
        callBack.onSelfClosed(this);
    }

    public interface ClientHandlerCallBack {
        /**
         * 关闭自己通知外部的回调
         *
         * @param clientHandler
         */
        void onSelfClosed(ClientHandler clientHandler);

        /**
         * 新信息到达的回调
         *
         * @param clientHandler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }

    class ClientReadHandler extends Thread {

        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            super.run();

            try {
                do {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            byteBuffer.clear();
                            int read = client.read(byteBuffer);
                            if (read > 0) {
                                //丢弃换行符
                                String msg = new String(byteBuffer.array(), 0, read - 2);
                                callBack.onNewMessageArrived(ClientHandler.this, msg);
                            } else {
                                log.warn("client can't read data");
                                ClientHandler.this.exitBySelf();
                                break;
                            }
                        }
                    }
                }while (!done);
            } catch (IOException e) {
                if (!done) {
                    log.info("unexpected disconnection from client. exception:{}", e.getMessage());
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                CloseUtil.close(selector);
            }

        }

        void exit() {
            done = true;
            selector.wakeup();
            CloseUtil.close(selector);
        }
    }


    class ClientWriteHandler extends Thread {

        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final LinkedBlockingQueue<String> queue;

        ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.queue = new LinkedBlockingQueue<>();
        }

        @Override
        public void run() {
            super.run();

            while (!done) {
                String msg = null;
                try {
                    msg = queue.take();
                } catch (InterruptedException ignore) {
                }
                if (msg != null) {
                   msg = msg+"\n";
                   byteBuffer.clear();
                   byteBuffer.put(msg.getBytes());
                   byteBuffer.flip();
                    while(!done && byteBuffer.hasRemaining()){
                        try {
                            int write = socketChannel.write(byteBuffer);
                            if(write<0){
                                log.warn("client can't write data");
                                ClientHandler.this.exitBySelf();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }

            }
        }

        void exit() {
            done = true;
            CloseUtil.close(selector);
            queue.clear();
        }

        void send(String msg) {
            queue.add(msg);
        }

    }
}
