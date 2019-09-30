package utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author zhengquan
 * @date 2019/9/22
 */
public class CloseUtil {

    public static void close(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
