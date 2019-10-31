package org.zhq.core;

import java.io.IOException;

public abstract class Frame {
    public static final int FRAME_HEADER_LENGTH = 6;
    public static final int MAX_CAPACITY = 64 * 1024 - 1;
    public static final byte TYPE_PACKET_HEADER = 11;
    public static final byte TYPE_PACKET_ENTITY = 12;
    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;
    public static final byte TYPE_COMMAND_HEART_BEAT = 81;
    public static final byte FLAG_NONE = 0;

    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    public Frame(int length, byte type, byte flag, short identifier) {
        checkParam(length, identifier);
        assembleHeader(length, type, flag, (byte) identifier);
    }

    public Frame(byte[]head) {
        System.arraycopy(head,0,header,0,FRAME_HEADER_LENGTH);
    }

    public int getBodyLength() {
        return ((((int) header[0]) & 0xFF) << 8) | (((int) header[1]) & 0xFF);
    }

    public byte getType() {
        return header[2];
    }

    public byte getFlag() {
        return header[3];
    }

    public short getIdentifier() {
        return (short) (((short)header[4]) & 0xFF);
    }

    public abstract boolean handle(IoArgs ioArgs) throws IOException;

    public abstract Frame nextFrame();

    private void assembleHeader(int length, byte type, byte flag, byte identifier) {
        header[0] = (byte) (length >> 8);
        header[1] = (byte) length;
        header[2] = type;
        header[3] = flag;
        header[4] = identifier;
        header[5] = 0;
    }

    private void checkParam(int length, short identifier) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException();
        }
        if (identifier < 1 || identifier > 255) {
            throw new RuntimeException();
        }
    }

    public abstract int getConsumableLength();
}
