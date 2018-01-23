package com.hikobe8.fakeimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.hikobe8.fakeimageloader.bitmap.ImageResizer;
import com.hikobe8.fakeimageloader.cache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by cherry on 2018/1/18.
 */

public class FakeImageLoader {

    //thread pool service info
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final Long KEEP_ALIVE = 10L;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, TAG + "load task" + mCount.getAndIncrement());
        }
    };
    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>();

    private static final long DISK_CACHE_SIZE = 50 * 1024 * 1024;
    private static final int DISK_CACHE_INDEX = 0;
    private static final String TAG = "FakeImageLoader";
    private static final int TAG_KEY_URI = R.id.key_tag_id;
    private static final int MSG_OBJ_RESULT = 0x2;
    private static final int IO_BUFFER_SIZE = 8*1024;
    private boolean mIsDiskLruCacheCreated;
    private Context mContext;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private ImageResizer mImageResizer;
    private ImageLoaderHandler mMainHandler;

    public void cancelAll() {
        THREAD_POOL_EXECUTOR.shutdown();
    }

    public interface LoadListener {
        void onLoadStart();

        void onLoadCompletely(Bitmap bitmap);

        void onLoadFailed();
    }

    private static class ImageLoaderHandler extends Handler {

        private WeakReference<Context> mContextWeakReference;

        public ImageLoaderHandler(Context context) {
            super(Looper.getMainLooper());
            mContextWeakReference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what != MSG_OBJ_RESULT) {
                return;
            }
            if (mContextWeakReference.get() == null)
                return;
            LoadedResult loadedResult = (LoadedResult) msg.obj;
            final String url = (String) loadedResult.mImageView.getTag(TAG_KEY_URI);
            if (loadedResult.mUri.equals(url)) {
                if (loadedResult.mBitmap == null) {
                    if (loadedResult.mLoadListener != null) {
                        loadedResult.mLoadListener.onLoadFailed();
                        return;
                    }
                }
                loadedResult.mImageView.setImageBitmap(loadedResult.mBitmap);
                if (loadedResult.mLoadListener != null) {
                    loadedResult.mLoadListener.onLoadCompletely(loadedResult.mBitmap);
                }
            } else {
                Log.w(TAG, "url has changed! ignore");
            }
        }

    }

    private ExecutorService THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS,
            sPoolWorkQueue,
            sThreadFactory);


    /**
     * build a new instance of ImageLoader
     * @param context
     * @return a new instance of ImageLoader
     */
    public static FakeImageLoader build(Context context) {
        return new FakeImageLoader(context);
    }

    private FakeImageLoader(Context context) {
        mContext = context.getApplicationContext();
        mMainHandler = new ImageLoaderHandler(context);
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
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
        mImageResizer = new ImageResizer();
    }

    private long getUsableSpace(File diskCacheDir) {
        return diskCacheDir.getFreeSpace();
    }

    private File getDiskCacheDir(Context context, String uniqueName) {

        boolean externalStorageAvailable = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    private Bitmap loadBitmapFromMemoryCache(String url) {
        String key = hashKeyFromUrl(url);
        return getBitmapFromMemoryCache(key);
    }

    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can't visit network from UI Thread.");
        }
        if (mDiskLruCache == null) {
            return null;
        }
        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadUrlToStream(url, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        return loadBitmapFromDisk(url, reqWidth, reqHeight);
    }

    private Bitmap loadBitmapFromDisk(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "load bitmap on UI thread is not recommended!");
        }
        if (mDiskLruCache == null) {
            return null;
        }
        String key = hashKeyFromUrl(url);
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if (snapshot != null) {
            FileInputStream inputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fd = inputStream.getFD();
            bitmap = mImageResizer.decodeBitmap(fd, reqWidth, reqHeight);
            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap);
            }
        }
        return bitmap;
    }

    private boolean downloadUrlToStream(String uri, OutputStream outputStream) {
        HttpURLConnection httpURLConnection =  null;
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream;
        try {
            URL url = new URL(uri);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), IO_BUFFER_SIZE);
            bufferedOutputStream = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
            int b;
            while ( (b = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(b);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "download bitmap failed. url : " + uri);
        } finally {
            //close
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }
            MyUtil.closeSilently(bufferedInputStream);
        }
        return false;
    }

    private String hashKeyFromUrl(String url) {
        String cacheKey;
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(url.getBytes());
            cacheKey = bytesToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public Bitmap loadBitmapSync(String url, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null) {
            Log.d(TAG, "load bitmap from memory cache, url = " + url);
            return bitmap;
        }
        try {
            bitmap = loadBitmapFromDisk(url, reqWidth, reqHeight);
            if (bitmap != null) {
                Log.d(TAG, "load bitmap from disk cache, url = " + url);
                return bitmap;
            }
            if (url.startsWith("file://")) {
                bitmap = loadBitmapLocal(url, reqWidth, reqHeight);
            } else {
                bitmap = loadBitmapFromHttp(url, reqWidth, reqHeight);
            }
            Log.d(TAG, "load bitmap from http, url = " + url);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (bitmap == null && !mIsDiskLruCacheCreated) {
            Log.w(TAG, "Encounter Error, DiskLruCache is not created.");
            bitmap = downloadBitmapFromUrl(url);
        }
        return bitmap;
    }

    private Bitmap loadBitmapLocal(String url, int reqWidth, int reqHeight) throws IOException, URISyntaxException {
        URI uri = new URI(url);
        url = uri.getPath();
        FileInputStream fileInputStream = new FileInputStream(url);
        FileDescriptor fd = fileInputStream.getFD();
        return mImageResizer.decodeBitmap(fd, reqWidth, reqHeight);
    }

    private Bitmap downloadBitmapFromUrl(String uri) {
        if (!uri.startsWith("http://")){
            return null;
        }
        HttpURLConnection httpURLConnection =  null;
        BufferedInputStream bufferedInputStream = null;
        Bitmap bitmap = null;
        try {
            URL url = new URL(uri);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(bufferedInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //close
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }
            MyUtil.closeSilently(bufferedInputStream);
        }
        return bitmap;
    }

    public void display(String url, ImageView imageView, int reqWidth, int reqHeight) {
        display(url, imageView, reqWidth, reqHeight, null);
    }

    public void display(final String url, final ImageView imageview, final int reqWidth, final int reqHeight, LoadListener loadListener) {
        imageview.setTag(TAG_KEY_URI, url);
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            return;
        }
        if (loadListener != null) {
            loadListener.onLoadStart();
        }
        final Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmapSync(url, reqWidth, reqHeight);
                if (bitmap != null) {
                    LoadedResult loadedResult = new LoadedResult(imageview, url, bitmap);
                    mMainHandler.obtainMessage(MSG_OBJ_RESULT, loadedResult).sendToTarget();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

}
