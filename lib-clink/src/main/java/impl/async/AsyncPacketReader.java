package impl.async;

import core.Frame;
import core.IoArgs;
import core.SendPacket;
import core.ds.BytePriorityNode;
import frames.AbsSendPacketFrame;
import frames.CancelSendFrame;
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


    boolean requestTakePacket() {
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

    synchronized void cancel(SendPacket packet) {
        if (nodeSize == 0) {
            return;
        }
        for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                //由于设计上这个队列中同时同一个包只会有一个帧存在因此遍历到一个帧就可以退出遍历了
                if (packetFrame.getPacket() == packet) {
                    boolean removable = packetFrame.abort();
                    if (removable) {
                        removeFrame(x, before);
                        if (packetFrame instanceof SendHeaderFrame) {
                            break;
                        }
                    }
                    CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getIdentifier());
                    appendNewFrame(cancelSendFrame);
                    provider.completedPacket(packet, false);
                    break;
                }
            }
        }
    }

    IoArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return null;
        }
        try {
            if (currentFrame.handle(ioArgs)) {
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }
                popCurrentFrame();
            }
            return ioArgs;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized void close(){
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbsSendPacketFrame) {
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                provider.completedPacket(packet, false);
            }
            node = node.next;
        }
        nodeSize = 0;
        node = null;
    }

    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (node == null) {
            node = newNode;
        } else {
            node.appendWithPriority(newNode);
        }
        nodeSize++;
    }

    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (before == null) {
            node = removeNode.next;
        } else {
            before.next = removeNode.next;
        }
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    private synchronized Frame getCurrentFrame() {
        return node == null ? null : node.item;
    }

    private short getIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }


    interface PacketProvider {
        SendPacket takePacket();

        void completedPacket(SendPacket packet, boolean succeed);
    }
}
