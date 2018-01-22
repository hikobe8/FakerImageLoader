package com.hikobe8.fakeimageloader;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by cherry on 2018/1/22.
 */

public class MyUtil {

    public static void closeSliently(Closeable closeable){
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
