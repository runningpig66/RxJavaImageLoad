package com.example.rxjavaimageload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.rxjavaimageload.model.Image;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkCacheObservable extends CacheObservable {
    @Override
    public Image getDataFromCache(String url) {
        Bitmap bitmap = downloadImage(url);//bitmap might be null
        return new Image(url, bitmap);
    }

    @Override
    public void putDataToCache(Image image) {
    }

    private Bitmap downloadImage(String urlString) {
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        Bitmap bitmap = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            inputStream = urlConnection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;//bitmap might be null
    }
}
