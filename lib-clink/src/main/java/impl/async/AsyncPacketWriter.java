package impl.async;

import core.Frame;
import core.IoArgs;
import core.ReceivePacket;
import frames.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AsyncPacketWriter implements Closeable {

    private final PacketProvider provider;

    private final Map<Short, PacketModel> map = new HashMap<>();

    private final IoArgs ioArgs = new IoArgs();

    private volatile Frame currentFrame;

    AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }

    synchronized IoArgs takeIoArgs() {
        ioArgs.limit(currentFrame == null ? Frame.FRAME_HEADER_LENGTH : currentFrame.getConsumableLength());
        return ioArgs;
    }

    synchronized void consumeIoArgs(IoArgs ioArgs) {
        if (currentFrame == null) {
            Frame temp;
            do {
                // 还有未消费数据，则重复构建帧
                temp = buildNewFrame(ioArgs);
            } while (temp == null && ioArgs.remained());

            if (temp == null) {
                // 最终消费数据完成，但没有可消费区间，则直接返回
                return;
            }

            currentFrame = temp;
            if (!ioArgs.remained()) {
                // 没有数据，则直接返回
                return;
            }
        }

        // 确保此时currentFrame一定不为null
        Frame currentFrame = this.currentFrame;
        do {
            try {
                if (currentFrame.handle(ioArgs)) {
                    // 某帧已接收完成
                    if (currentFrame instanceof ReceiveHeaderFrame) {
                        // Packet 头帧消费完成，则根据头帧信息构建接收的Packet
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                        ReceivePacket packet = provider.takePacket(headerFrame.getPacketType(),
                                headerFrame.getPacketLength(),
                                headerFrame.getPacketHeaderInfo());
                        appendNewPacket(headerFrame.getIdentifier(), packet);
                    } else if (currentFrame instanceof ReceiveEntityFrame) {
                        // Packet 实体帧消费完成，则将当前帧消费到Packet
                        completeEntityFrame((ReceiveEntityFrame) currentFrame);
                    }

                    // 接收完成后，直接推出循环，如果还有未消费数据则交给外层调度
                    currentFrame = null;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (ioArgs.remained());
    }

    private void completeEntityFrame(ReceiveEntityFrame entityFrame) {
        synchronized (map) {
            short identifier = entityFrame.getIdentifier();
            int length = entityFrame.getBodyLength();
            PacketModel packetModel = map.get(identifier);
            packetModel.unreceivedLength = -length;
            if (packetModel.unreceivedLength == 0) {
                provider.completedPacket(packetModel.packet, true);
                map.remove(identifier);
            }
        }
    }


    @Override
    public void close() {
        synchronized (map) {
            Collection<PacketModel> packetModels = map.values();
            for (PacketModel packetModel : packetModels) {
                provider.completedPacket(packetModel.packet, false);
            }
            map.clear();
        }
    }


    private Frame buildNewFrame(IoArgs ioArgs) {
        AbsReceiveFrame newFrame = ReceiveFrameFactory.createNewFrame(ioArgs);
        if (newFrame instanceof CancelReceiveFrame) {
            cancelReceivePacket(newFrame.getIdentifier());
            return null;
        } else if (newFrame instanceof ReceiveEntityFrame) {
            WritableByteChannel channel = getPacketChannel(newFrame.getIdentifier());
            ((ReceiveEntityFrame) newFrame).bindPacketChannel(channel);
        }
        return newFrame;
    }

    private WritableByteChannel getPacketChannel(short identifier) {
        synchronized (map) {
            PacketModel packetModel = map.get(identifier);
            return packetModel==null?null:packetModel.channel;
        }
    }

    private void cancelReceivePacket(short identifier) {
        synchronized (map) {
            PacketModel packetModel = map.get(identifier);
            if (packetModel != null) {
                ReceivePacket packet = packetModel.packet;
                provider.completedPacket(packet, false);
            }
        }
    }


    private void appendNewPacket(short identifier, ReceivePacket receivePacket) {
        synchronized (map) {
            PacketModel packetModel = new PacketModel(receivePacket);
            map.put(identifier, packetModel);
        }
    }

    interface PacketProvider {
        ReceivePacket takePacket(byte type, long length, byte[] headerInfo);

        void completedPacket(ReceivePacket packet, boolean succeed);
    }

    private class PacketModel {
        private final ReceivePacket packet;
        private final WritableByteChannel channel;
        private volatile long unreceivedLength;

        public PacketModel(ReceivePacket<?, ?> packet) {
            this.packet = packet;
            this.channel = Channels.newChannel(packet.open());
            this.unreceivedLength = packet.length();
        }
    }
}
