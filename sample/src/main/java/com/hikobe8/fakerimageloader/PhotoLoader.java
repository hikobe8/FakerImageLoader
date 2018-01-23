package com.hikobe8.fakerimageloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: yr
 * 18-1-23 下午2:40.
 */

public class PhotoLoader {

    private Context mContext;
    private OnPhotosLoadFinishedListener mOnPhotosLoadFinishedListener;

    public PhotoLoader(Context context, OnPhotosLoadFinishedListener listener) {
        mContext = context;
        mOnPhotosLoadFinishedListener = listener;
    }

    public interface OnPhotosLoadFinishedListener {
        void onPhotoLoadFinished(PhotoLoadedBean photoLoadedBean);
    }

    public void fetchAllPhotos(){
        @SuppressLint("StaticFieldLeak")
        AsyncTask<Void, Void, PhotoLoadedBean> loadPhotosTask = new LoadPhotosTask(mContext) {
            @Override
            void onPhotosLoaded(PhotoLoadedBean photoLoadedBean) {
                if (mOnPhotosLoadFinishedListener != null) {
                    mOnPhotosLoadFinishedListener.onPhotoLoadFinished(photoLoadedBean);
                }
            }

        };
        loadPhotosTask.execute();
    }

    static class PhotoLoadedBean {
        public final List<String> mPhotos = new ArrayList<>();

        public void addPhoto(String path) {
            mPhotos.add(path);
        }
    }
}
