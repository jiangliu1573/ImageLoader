package com.cosmos.mark.imageloader.cache;

import android.graphics.Bitmap;

/**
 * Created by jiangliu on 2015/12/13.
 */
public interface ImageCache {
    Bitmap get(String key);
    Bitmap get(String key, int reqWidth,
               int reqHeight);
    void put(String key, Bitmap bitmap);
}
