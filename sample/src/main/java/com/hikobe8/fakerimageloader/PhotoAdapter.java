package com.hikobe8.fakerimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hikobe8.fakeimageloader.FakeImageLoader;

import java.util.List;

/**
 * Author: yr
 * 18-1-23 下午4:21.
 */

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.MViewHolder> {

    FakeImageLoader mFakeImageLoader;

    private List<String> mPath;
    private boolean mShouldPreventImageLoading;
    public PhotoAdapter(Context context, List<String> path) {
        mFakeImageLoader = FakeImageLoader.build(context);
        mPath = path;
    }

    @Override
    public MViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new MViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mPath.size();
    }

    @Override
    public void onBindViewHolder(MViewHolder holder, int position) {
        holder.bindData(mPath.get(position));
    }

    public void notifyPreventImageLoading(boolean prevent) {
        mShouldPreventImageLoading = prevent;
    }

    public class MViewHolder extends RecyclerView.ViewHolder{

        private SquareImageView mSquareImageView;

        public MViewHolder(View itemView) {
            super(itemView);
            mSquareImageView = (SquareImageView) itemView;
        }

        public void bindData(String path){
            if (mShouldPreventImageLoading) {
                mFakeImageLoader.cancelAll();
                return;
            }
            mFakeImageLoader.display(path, mSquareImageView, 100, 100, new FakeImageLoader.LoadListener() {
                @Override
                public void onLoadStart() {
                    mSquareImageView.setImageBitmap(null);
                }

                @Override
                public void onLoadCompletely(Bitmap bitmap) {

                }

                @Override
                public void onLoadFailed() {

                }
            });
        }

    }

}
