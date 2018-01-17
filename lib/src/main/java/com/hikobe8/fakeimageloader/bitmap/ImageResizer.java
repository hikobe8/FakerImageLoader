package com.hikobe8.fakeimageloader.bitmap;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * 图片压缩工具类
 * Created by Ray on 2018/1/16.
 */

public class ImageResizer {

    private static final String TAG = ImageResizer.class.getSimpleName();

    public ImageResizer(){}

    public Bitmap decodeBitmap(Resources resources, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options ops = new BitmapFactory.Options();
        ops.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, ops);
        ops.inSampleSize = calculateInSampleSize(ops, reqWidth, reqHeight);
        ops.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resId, ops);
    }

    public Bitmap decodeBitmp(FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {
        BitmapFactory.Options ops = new BitmapFactory.Options();
        ops.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, ops);
        ops.inSampleSize = calculateInSampleSize(ops, reqWidth, reqHeight);
        ops.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, ops);
    }

    private int calculateInSampleSize(BitmapFactory.Options ops, int reqWidth, int reqHeight) {
        if (reqWidth == 0 || reqHeight == 0)
            return 1;
        int rawWidth = ops.outWidth;
        int rawHeight = ops.outHeight;
        Log.d(TAG, "image raw width = " + rawWidth + ", height = " + rawHeight);
        int inSampleSize = 1;
        if (rawWidth > reqWidth || rawHeight > reqHeight) {
            int halfWidth = rawWidth / 2;
            int halfHeight = rawHeight / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize*=2;
            }
        }
        Log.d(TAG, "sampleSize = " + inSampleSize);
        return inSampleSize;
    }

}
