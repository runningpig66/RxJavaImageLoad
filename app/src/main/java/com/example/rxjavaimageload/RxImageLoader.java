package com.example.rxjavaimageload;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.example.rxjavaimageload.model.Image;

import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;

public class RxImageLoader {
    private static final String TAG = "RxImageLoader";
    private static RxImageLoader singleton;
    private String mUrl;
    private RequestCreator mRequestCreator;

    private RxImageLoader(Builder builder) {
        mRequestCreator = new RequestCreator(builder.mContext);
    }

    public static RxImageLoader with(Context context) {
        if (singleton == null) {
            synchronized (RxImageLoader.class) {
                if (singleton == null) {
                    singleton = new Builder(context).build();
                }
            }
        }
        return singleton;
    }

    public RxImageLoader load(String url) {
        mUrl = url;
        return singleton;
    }

    public void into(ImageView imageView) {
        Observable.concat(mRequestCreator.getImageFromMemory(mUrl),
                mRequestCreator.getImageFromDisk(mUrl),
                mRequestCreator.getImageFromNetwork(mUrl))
                .filter(new Predicate<Image>() {
                    @Override
                    public boolean test(Image image) throws Exception {
                        return image.getBitmap() != null;
                    }
                })
                .firstElement()
                .subscribe(new MaybeObserver<Image>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(Image image) {
                        imageView.setImageBitmap(image.getBitmap());
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: ");
                    }
                });
    }

    public static class Builder {
        private Context mContext;

        public Builder(Context context) {
            mContext = context;
        }

        public RxImageLoader build() {
            return new RxImageLoader(this);
        }
    }
}
