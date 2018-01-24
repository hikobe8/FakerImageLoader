package com.hikobe8.fakerimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
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

    private FakeImageLoader mFakeImageLoader;

    private List<String> mPath;

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
        Log.e("test adapter", "onBindViewHolder : " + position);
        holder.bindData(mPath.get(position));
    }

    public class MViewHolder extends RecyclerView.ViewHolder{

        private SquareImageView mSquareImageView;

        public MViewHolder(View itemView) {
            super(itemView);
            mSquareImageView = (SquareImageView) itemView;
        }

        public void bindData(final String path){
            String tagPath = (String) mSquareImageView.getTag(R.id.recycler);
            if (!TextUtils.equals(tagPath, path)) {
                mSquareImageView.setImageResource(R.drawable.default_loading_bg);
                mFakeImageLoader.display(path, mSquareImageView, 100, 100);
            }
        }

    }

}
