package com.cosmos.mark.imageloader.cache;

import android.content.Context;
import android.graphics.Bitmap;

import com.cosmos.mark.imageloader.DiskLruCache;
import com.cosmos.mark.imageloader.utils.DiskUtil;
import com.cosmos.mark.imageloader.utils.IOUtil;
import com.cosmos.mark.imageloader.utils.ImageUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by jiangliu on 2015/12/13.
 */
public class DiskCache implements ImageCache {

    private static final int DISK_CACHE_INDEX = 0;

    /**
     * sd卡缓存.
     */
    private DiskLruCache mDiskLruCache;


    /**
     * sdk卡缓存大小.
     */
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;

    public DiskCache(Context context){
        File diskCacheDir = DiskUtil.getDiskCacheDir(context, "bitmap");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        if (DiskUtil.getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1,
                        DISK_CACHE_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public Bitmap get(String key) {
       return null;
    }

    @Override
    public Bitmap get(String key, int reqWidth, int reqHeight) {
        if (mDiskLruCache == null) {
            return null;
        }

        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapShot = null;
        try {
            snapShot = mDiskLruCache.get(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (snapShot != null) {
            FileInputStream fileInputStream = (FileInputStream) snapShot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = null;
            try {
                fileDescriptor = fileInputStream.getFD();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bitmap = ImageUtil.decodeSampledBitmapFromFileDescriptor(fileDescriptor,
                    reqWidth, reqHeight);
        }

        return bitmap;
    }

    @Override
    public void put(String key, Bitmap bitmap) {
        try {
            if(mDiskLruCache == null || mDiskLruCache.get(key) != null){
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        DiskLruCache.Editor editor;
        try {
            editor = mDiskLruCache.edit(key);
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
                if (writeBitmapToDisk(bitmap, outputStream)) {
                    editor.commit();
                } else {
                    editor.abort();
                }
                mDiskLruCache.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean writeBitmapToDisk(Bitmap bitmap, OutputStream outputStream) {
        BufferedOutputStream bos = new BufferedOutputStream(outputStream, 8 * 1024);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        boolean result = true;
        try {
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        } finally {
            IOUtil.close(bos);
        }

        return result;
    }
}
