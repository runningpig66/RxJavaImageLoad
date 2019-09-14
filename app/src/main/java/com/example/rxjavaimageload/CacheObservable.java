package com.example.rxjavaimageload;

import com.example.rxjavaimageload.model.Image;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

//缓存Observable抽象父类
public abstract class CacheObservable {

    public Observable<Image> getImage(String url) {
        return Observable.create(new ObservableOnSubscribe<Image>() {
            @Override
            public void subscribe(ObservableEmitter<Image> emitter) throws Exception {
                if (!emitter.isDisposed()) {
                    Image image = getDataFromCache(url);
                    emitter.onNext(image);
                    emitter.onComplete();
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public abstract Image getDataFromCache(String url);

    public abstract void putDataToCache(Image image);
}
