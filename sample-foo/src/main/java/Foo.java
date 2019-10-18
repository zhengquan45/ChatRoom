import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Foo {
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
