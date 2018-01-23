package com.hikobe8.fakerimageloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: yr
 * 18-1-23 下午4:28.
 */

public class PhotoLoadedBean {
    public final List<String> mPhotos = new ArrayList<>();

    public void addPhoto(String path) {
        mPhotos.add(path);
    }
}
