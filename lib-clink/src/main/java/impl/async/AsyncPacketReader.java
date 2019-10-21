package impl.async;

import core.Frame;
import core.IoArgs;
import core.SendPacket;
import core.ds.BytePriorityNode;
import frames.SendEntityFrame;
import frames.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;

public class AsyncPacketReader implements Closeable {

    private volatile IoArgs ioArgs = new IoArgs();
    private final PacketProvider provider;

    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    public void cancel(SendPacket packet) {

    }

    public boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                return true;
            }
        }
        SendPacket packet = provider.takePacket();
        if (packet != null) {
            short identifier = getIdentifier();
            SendHeaderFrame sendHeaderFrame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(sendHeaderFrame);
        }
        synchronized (this) {
            return nodeSize != 0;
        }
    }


    @Override
    public void close() throws IOException {

    }

    public IoArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return null;
        }
        try {
            if (currentFrame.handle(ioArgs)) {
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                }else if(currentFrame instanceof SendEntityFrame){
                    provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(),true);
                }
                popCurrentFrame();
            }
            return ioArgs;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void popCurrentFrame() {

    }


    private short getIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    private void appendNewFrame(Frame frame) {

    }

    private Frame getCurrentFrame() {
        return null;
    }

    interface PacketProvider {
        SendPacket takePacket();

        void completedPacket(SendPacket packet, boolean succeed);
    }
}
