package org.zhq.core;

import java.io.Closeable;

public interface SendDispatcher extends Closeable {
    void send(SendPacket sendPacket);
    void sendHeartBeat();
    void cancel(SendPacket sendPacket);
}
