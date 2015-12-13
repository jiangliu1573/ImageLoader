package com.cosmos.mark.imageloader.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by jiangliu on 2015/12/13.
 */
public class IOUtil {
    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
