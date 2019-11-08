package org.zhq;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Foo {

    public static final String COMMAND_CONNECTOR_BIND="--m c bind ";
    public static final String COMMAND_AUDIO_CREATE_ROOM="--m a create";
    public static final String COMMAND_AUDIO_JOIN_ROOM="--m a join ";
    public static final String COMMAND_AUDIO_LEAVE_ROOM="--m a leave";

    public static final String COMMAND_INFO_NAME="--i server ";
    public static final String COMMAND_INFO_AUDIO_ROOM="--i a room ";
    public static final String COMMAND_INFO_AUDIO_START="--i a start ";
    public static final String COMMAND_INFO_AUDIO_STOP="--i a stop";
    public static final String COMMAND_INFO_AUDIO_ERROR="--i a error";



    public static final String COMMAND_EXIT = "00bye00";
    public static final String COMMAND_GROUP_JOIN = "--m g join ";
    public static final String COMMAND_GROUP_LEAVE = "--m g leave ";
    public static final String CACHE_DIR = "cache";

    public static File getCacheDir(String dir) {
        String path = new StringBuilder().append(System.getProperty("user.dir")).append(File.separator).append(CACHE_DIR).append(File.separator).append(dir).toString();
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("create path error");
            }
        }
        return file;
    }

    public static File createRandomTemp(File parent) {
        String string = UUID.randomUUID().toString() + ".tmp";
        File file = new File(parent, string);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
