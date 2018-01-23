package com.hikobe8.fakerimageloader;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.lang.ref.WeakReference;

/**
 * Author: yr
 * 18-1-23 下午3:26.
 */

public abstract class LoadPhotosTask extends AsyncTask<Void, Void, PhotoLoader.PhotoLoadedBean> {

    private WeakReference<Context> mContextWeakReference;

    public LoadPhotosTask(Context context) {
        mContextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected PhotoLoader.PhotoLoadedBean doInBackground(Void... voids) {
        Context context = mContextWeakReference.get();
        if (context == null)
            return null;
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Images.Media.MIME_TYPE + " = ? or " + MediaStore.Images.Media.MIME_TYPE + " = ?",
                new String[]{"image/png", "image/jpeg"}, null);
        PhotoLoader.PhotoLoadedBean photoLoadedBean = new PhotoLoader.PhotoLoadedBean();
        while (cursor.moveToNext()) {
            photoLoadedBean.addPhoto("file://" + cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
        }
        cursor.close();
        return photoLoadedBean;
    }

    @Override
    protected void onPostExecute(PhotoLoader.PhotoLoadedBean photoLoadedBean) {
        super.onPostExecute(photoLoadedBean);
        if (mContextWeakReference.get() != null)
            onPhotosLoaded(photoLoadedBean);
    }

    abstract void onPhotosLoaded(PhotoLoader.PhotoLoadedBean photoLoadedBean);

}
