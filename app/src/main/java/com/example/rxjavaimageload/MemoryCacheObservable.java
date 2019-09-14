package com.example.rxjavaimageload;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.example.rxjavaimageload.model.Image;

public class MemoryCacheObservable extends CacheObservable {
    private int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private int cacheSize = maxMemory / 8;
    private LruCache<String, Bitmap> mLruCache = new LruCache<String, Bitmap>(cacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getRowBytes() * value.getHeight() / 1024;
        }
    };

    @Override
    public Image getDataFromCache(String url) {
        Bitmap bitmap = mLruCache.get(url);
        return new Image(url, bitmap);
    }

    @Override
    public void putDataToCache(Image image) {
        mLruCache.put(image.getUrl(), image.getBitmap());
    }
}
