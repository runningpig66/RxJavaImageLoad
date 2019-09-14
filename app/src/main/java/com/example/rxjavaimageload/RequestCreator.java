package com.example.rxjavaimageload;

import android.content.Context;
import android.util.Log;

import com.example.rxjavaimageload.model.Image;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

public class RequestCreator {
    private static final String TAG = "RequestCreator";
    private MemoryCacheObservable memoryCacheObservable;
    private DiskCacheObservable diskCacheObservable;
    private NetworkCacheObservable networkCacheObservable;

    public RequestCreator(Context context) {
        memoryCacheObservable = new MemoryCacheObservable();
        diskCacheObservable = new DiskCacheObservable(context);
        networkCacheObservable = new NetworkCacheObservable();
    }

    public Observable<Image> getImageFromMemory(String url) {
        return memoryCacheObservable.getImage(url)
                .filter(new Predicate<Image>() {
                    @Override
                    public boolean test(Image image) throws Exception {
                        return image.getBitmap() != null;
                    }
                })
                .doOnNext(new Consumer<Image>() {
                    @Override
                    public void accept(Image image) throws Exception {
                        Log.d(TAG, "accept: get data from memory");
                    }
                });
    }

    public Observable<Image> getImageFromDisk(String url) {
        return diskCacheObservable.getImage(url)
                .filter(new Predicate<Image>() {
                    @Override
                    public boolean test(Image image) throws Exception {
                        return image.getBitmap() != null;
                    }
                })
                .doOnNext(new Consumer<Image>() {
                    @Override
                    public void accept(Image image) throws Exception {
                        Log.d(TAG, "accept: get data from disk");
                        if (image.getBitmap() != null) {
                            memoryCacheObservable.putDataToCache(image);
                        }
                    }
                });
    }

    public Observable<Image> getImageFromNetwork(String url) {
        return networkCacheObservable.getImage(url)
                .filter(new Predicate<Image>() {
                    @Override
                    public boolean test(Image image) throws Exception {
                        return image.getBitmap() != null;
                    }
                })
                .doOnNext(new Consumer<Image>() {
                    @Override
                    public void accept(Image image) throws Exception {
                        Log.d(TAG, "accept: get data from network");
                        if (image.getBitmap() != null) {
                            diskCacheObservable.putDataToCache(image);
                            memoryCacheObservable.putDataToCache(image);
                        }
                    }
                });
    }
}
