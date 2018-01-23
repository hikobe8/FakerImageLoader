package com.hikobe8.fakerimageloader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements PhotoLoader.OnPhotosLoadFinishedListener{


    private RecyclerView mRecyclerView;
    private PhotoAdapter mPhotoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                mPhotoAdapter.notifyPreventImageLoading(newState != RecyclerView.SCROLL_STATE_IDLE);
            }
        });
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
        }
        new PhotoLoader(this, this).fetchAllPhotos();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            new PhotoLoader(this, this).fetchAllPhotos();
            return;
        }
        finish();
    }

    @Override
    public void onPhotoLoadFinished(PhotoLoader.PhotoLoadedBean photoLoadedBean) {
        if (photoLoadedBean.mPhotos.size() > 0) {
            mPhotoAdapter = new PhotoAdapter(this, photoLoadedBean.mPhotos);
            mRecyclerView.setAdapter(mPhotoAdapter);
        }
    }
}
