package com.example.rxjavaimageload;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.rxjavaimageload.disk.DiskLruCache;
import com.example.rxjavaimageload.disk.Util;
import com.example.rxjavaimageload.model.Image;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

public class DiskCacheObservable extends CacheObservable {
    private static final String TAG = "DiskCacheObservable";
    private static final int MAX_SIZE = 20 * 1024 * 1024;
    private DiskLruCache mDiskLruCache;
    private Context mContext;

    public DiskCacheObservable(Context mContext) {
        this.mContext = mContext;
        initDiskLruCache();
    }

    @Override
    public Image getDataFromCache(String url) {
        Bitmap bitmap = getCache(url);
        return new Image(url, bitmap);
    }

    @Override
    public void putDataToCache(Image image) {
        //有下载，放子线程执行
        Observable.create(new ObservableOnSubscribe<Image>() {
            @Override
            public void subscribe(ObservableEmitter<Image> emitter) throws Exception {
                putDataToDiskLruCache(image);
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private void initDiskLruCache() {
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            try {
                File cacheDir = Util.getDiskCacheDir(mContext, "CacheDir");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                int versionCode = Util.getAppVersionCode(mContext);
                mDiskLruCache = DiskLruCache.open(cacheDir, versionCode, 1, MAX_SIZE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getCache(String url) {
        try {
            String key = Util.hashKeyForDisk(url);
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                InputStream in = snapshot.getInputStream(0);
                return BitmapFactory.decodeStream(in);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //与getCache()功能一樣
    private Bitmap getDataFromDiskLruCache(String url) {
        FileInputStream fileInputStream = null;
        Bitmap bitmap = null;
        //生成图片URL对应的key
        final String key = Util.hashKeyForDisk(url);
        try {
            //查找key对应的缓存
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                fileInputStream = (FileInputStream) snapshot.getInputStream(0);
                FileDescriptor fileDescriptor = fileInputStream.getFD();
                //将缓存数据解析成Bitmap对象
                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;//bitmap might be null
    }

    private void putDataToDiskLruCache(Image image) {
        try {
            //第一步：获取将要缓存的图片的对应的唯一key值
            String key = Util.hashKeyForDisk(image.getUrl());
            //第二步：获取DiskLruCache的Editor
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor != null) {
                //第三步：从Editor中获取OutputStream
                OutputStream outputStream = editor.newOutputStream(0);
                //第四步：下载网络图片然后保存至DiskLruCache图片缓存中
                if (downloadUrlToStream(image.getUrl(), outputStream)) {
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
            mDiskLruCache.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int IO_BUFFER_SIZE = 8 * 1024;//8kb

    /**
     * Download a bitmap from a URL and write the content to an output stream.
     *
     * @param urlString The URL to fetch
     * @return true if successful, false otherwise
     */
    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
