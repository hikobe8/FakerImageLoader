package com.hikobe8.fakeimageloader;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by cherry on 2018/1/22.
 */

public class MyUtil {

    public static void closeSilently(Closeable closeable){
        try {
            if (closeable != null)
                closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
