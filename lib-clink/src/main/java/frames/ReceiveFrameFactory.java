package frames;

import core.Frame;
import core.IoArgs;

public class ReceiveFrameFactory {

    public static AbsReceiveFrame createNewFrame(IoArgs ioArgs) {
        byte[] buffer = new byte[Frame.FRAME_HEADER_LENGTH];
        ioArgs.writeTo(buffer, 0);
        //read 6 bytes include type
        byte type = buffer[2];
        switch (type) {
            case Frame.TYPE_COMMAND_RECEIVE_REJECT:
                return new CancelReceiveFrame(buffer);
            case Frame.TYPE_PACKET_HEADER:
                return new ReceiveHeaderFrame(buffer);
            case Frame.TYPE_PACKET_ENTITY:
                return new ReceiveEntityFrame(buffer);
            default:
                throw new UnsupportedOperationException("Unsupported Frame type:" + type);
        }
    }
}
