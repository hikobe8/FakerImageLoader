package com.hikobe8.fakeimageloader;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

/**
 * Created by cherry on 2018/1/20.
 */

public class LoadedResult implements Parcelable {

    public ImageView mImageView;
    public String mUri;
    public Bitmap mBitmap;

    public LoadedResult(ImageView imageView, String uri, Bitmap bitmap) {
        mImageView = imageView;
        mUri = uri;
        mBitmap = bitmap;
    }

    protected LoadedResult(Parcel in) {
        mUri = in.readString();
        mBitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<LoadedResult> CREATOR = new Creator<LoadedResult>() {
        @Override
        public LoadedResult createFromParcel(Parcel in) {
            return new LoadedResult(in);
        }

        @Override
        public LoadedResult[] newArray(int size) {
            return new LoadedResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUri);
        dest.writeParcelable(mBitmap, flags);
    }
}
