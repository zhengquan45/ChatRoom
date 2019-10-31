package org.zhq.box;

import org.zhq.core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {
    private final File file;
    public FileReceivePacket(long len, File file) {
        super(len);
        this.file = file;
    }

    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_FILE;
    }
}
