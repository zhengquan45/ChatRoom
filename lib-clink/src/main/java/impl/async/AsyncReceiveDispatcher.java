package impl.async;

import box.StringReceivePacket;
import core.IoArgs;
import core.ReceiveDispatcher;
import core.ReceivePacket;
import core.Receiver;
import utils.CloseUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallBack callBack;

    private IoArgs args = new IoArgs();
    private ReceivePacket curReceivePacket;
    private byte[] buffer;
    private int total;
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallBack callBack) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(ioArgsEventListener);
        this.callBack = callBack;
    }

    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive() {
        try {
            receiver.receiveAsync(args);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void stop() {

    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {

        @Override
        public void onStarted(IoArgs args) {
            int receiveSize;
            if (curReceivePacket == null) {
                receiveSize = 4;
            } else {
                receiveSize = Math.min(total-position,args.capacity());
            }
            args.limit(receiveSize);
        }

        @Override
        public void onCompleted(IoArgs args) {
            assemblePacket(args);
            registerReceive();
        }
    };

    private void assemblePacket(IoArgs args) {
        //包头
        if(curReceivePacket==null){
            int length = args.readLength();
            curReceivePacket = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }
        int count = args.writeTo(buffer,0);
        if(count >0){
            curReceivePacket.save(buffer,count);
            position+=count;
            if(position==total){
                completePacket();
                curReceivePacket = null;
            }
        }

    }

    private void completePacket() {
        ReceivePacket packet = this.curReceivePacket;
        CloseUtil.close(packet);
        callBack.onReceivePacketCompleted(packet);
    }

    @Override
    public void close() throws IOException {
        if(closed.compareAndSet(false,true)){
            if(curReceivePacket!=null){
                CloseUtil.close(curReceivePacket);
                curReceivePacket = null;
            }
        }
    }
}
