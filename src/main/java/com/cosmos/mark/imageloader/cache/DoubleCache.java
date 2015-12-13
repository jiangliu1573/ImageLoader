package com.cosmos.mark.imageloader.cache;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by jiangliu on 2015/12/13.
 */
public class DoubleCache implements ImageCache {

    MemoryCache mMemoryCache;
    DiskCache mDiskCache;

    public DoubleCache(Context context) {
        mMemoryCache = new MemoryCache();
        mDiskCache = new DiskCache(context);
    }

    @Override
    public Bitmap get(String key) {
        return null;
    }

    @Override
    public Bitmap get(String key, int reqWidth, int reqHeight) {
        Bitmap bitmap = mMemoryCache.get(key);
        if (bitmap == null) {
            bitmap = mDiskCache.get(key, reqWidth, reqHeight);
        }
        return bitmap;
    }

    @Override
    public void put(String key, Bitmap bitmap) {
        mMemoryCache.put(key, bitmap);
        mDiskCache.put(key, bitmap);
    }
}
