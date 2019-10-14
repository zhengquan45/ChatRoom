package core;

import java.io.Closeable;

public interface SendDispatcher extends Closeable {
    void send(SendPacket sendPacket);
    void cancel(SendPacket sendPacket);
}
