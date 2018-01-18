package com.hikobe8.fakeimageloader.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.LruCache;

import com.hikobe8.fakeimageloader.cache.DiskLruCache;

import java.io.File;
import java.io.IOException;

/**
 * Created by cherry on 2018/1/18.
 */

public class FakeImageLoader {


    private static final long DISK_CACHE_SIZE = 50 * 1024 * 1024;
    private boolean mIsDiskLruCacheCreated;
    private Context mContext;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    private FakeImageLoader(Context context) {
        mContext = context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String,Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private long getUsableSpace(File diskCacheDir) {
        return diskCacheDir.getFreeSpace() / 1024;
    }

    private File getDiskCacheDir(Context context, String bitmap) {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        return new File(externalStorageDirectory, bitmap);
    }

}
