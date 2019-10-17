package box;

import core.SendPacket;

import java.io.File;
import java.io.FileInputStream;

public class FileSendPacket extends SendPacket<FileInputStream> {

    public FileSendPacket(File file) {
        setLength(file.length());
    }

    @Override
    protected FileInputStream createStream() {
        return null;
    }
}
