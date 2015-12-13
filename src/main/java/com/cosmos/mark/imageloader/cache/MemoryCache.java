package com.cosmos.mark.imageloader.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by jiangliu on 2015/12/13.
 */
public class MemoryCache implements ImageCache {
    /**
     * 内存缓存.
     */
    private LruCache<String, Bitmap> mLruCache;

    public MemoryCache(){
        // 初始化缓存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    @Override
    public Bitmap get(String key) {
        return mLruCache.get(key);
    }

    @Override
    public Bitmap get(String key, int reqWidth, int reqHeight) {
        return null;
    }

    @Override
    public void put(String key, Bitmap bitmap) {
        if (get(key) == null) {
            if (bitmap != null)
                mLruCache.put(key, bitmap);
        }
    }
}
