package com.cosmos.mark.imageloader.config;

import com.cosmos.mark.imageloader.ImageLoader;
import com.cosmos.mark.imageloader.cache.ImageCache;

public class ImageLoaderConfiguration {

    public int threadCount;

    public ImageCache imageCache;

    public ImageLoader.Type type;

    public ImageLoaderConfiguration setThreadCount(int count) {
        threadCount = Math.max(1, count);
        return this;
    }

    public ImageLoaderConfiguration setmImageCache(ImageCache mImageCache) {
        this.imageCache = mImageCache;
        return this;
    }

    public ImageLoaderConfiguration setmType(ImageLoader.Type mType) {
        this.type = mType;
        return this;
    }
}
