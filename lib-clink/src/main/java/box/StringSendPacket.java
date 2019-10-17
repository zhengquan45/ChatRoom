package box;

public class StringSendPacket extends BytesSendPacket {

    public StringSendPacket(String msg) {
       super(msg.getBytes());
    }

    @Override
    protected byte type() {
        return TYPE_MEMORY_STRING;
    }
}
