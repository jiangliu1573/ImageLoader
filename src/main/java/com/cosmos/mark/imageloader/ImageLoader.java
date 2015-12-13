package com.cosmos.mark.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.cosmos.mark.imageloader.cache.DoubleCache;
import com.cosmos.mark.imageloader.cache.ImageCache;
import com.cosmos.mark.imageloader.config.DisplayConfig;
import com.cosmos.mark.imageloader.config.ImageLoaderConfiguration;
import com.cosmos.mark.imageloader.utils.IOUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ImageLoader {

    public static final String TAG = ImageLoader.class.getSimpleName();

    /**
     * 线程池.
     */
    private ExecutorService mThreadPool;

    /**
     * 任务队列.
     */
    private LinkedList<Runnable> mTasks;

    /**
     * 轮询的线程.
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHander;

    /**
     * 运行在UI线程的handler，用于给ImageView设置图片.
     */
    private Handler mUiHandler;

    /**
     * 引入一个值为1的信号量，防止mPoolThreadHander未初始化完成.
     */
    private volatile Semaphore mSemaphore = new Semaphore(1);

    private volatile Semaphore mPoolSemaphore;

    private static ImageLoader mInstance;

    private ImageCache mImageCache;

    private ImageLoaderConfiguration mConfig;

    /**
     * 队列的调度方式
     *
     * @author zhy
     */
    public enum Type {
        FIFO, LIFO
    }


    /**
     * 单例获得该实例对象
     *
     * @return
     */
    public static ImageLoader getInstance() {

        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader();
                }
            }
        }
        return mInstance;
    }


    private void checkConfig(ImageLoaderConfiguration configuration) {

        if (configuration == null) {
            throw new RuntimeException(
                    "The config of SimpleImageLoader is Null, please call the init(ImageLoaderConfig config) method to initialize");
        }

        mConfig = configuration;

        if (mConfig.type == null) {
            mConfig.type = Type.LIFO;
        }

        if(mConfig.threadCount <= 0){
            mConfig.threadCount = Runtime.getRuntime().availableProcessors();
        }

        mImageCache = mConfig.imageCache;
    }

    public void init(Context context, ImageLoaderConfiguration configuration) {

        checkConfig(configuration);

        if( mImageCache == null){
            mImageCache = new DoubleCache(context);
        }

        mUiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                ImageView imageView = holder.imageView;
                Bitmap bm = holder.bitmap;
                String path = holder.path;
                if (imageView.getTag().toString().equals(path)) {
                    imageView.setImageBitmap(bm);
                }else {
                    Log.w(TAG, "set image bitmap,but url has changed, ignored!");
                }
            }
        };

        // loop thread
        mPoolThread = new Thread() {
            @Override
            public void run() {
                try {
                    // 请求一个信号量
                    mSemaphore.acquire();
                } catch (InterruptedException e) {
                }
                Looper.prepare();

                mPoolThreadHander = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        try {
                            mPoolSemaphore.acquire();
                            mThreadPool.execute(getTask());
                        } catch (InterruptedException e) {
                        }
                    }
                };
                // 释放一个信号量
                mSemaphore.release();
                Looper.loop();
            }
        };
        mPoolThread.start();


        mThreadPool = Executors.newFixedThreadPool(configuration.threadCount);
        mPoolSemaphore = new Semaphore(configuration.threadCount);
        mTasks = new LinkedList<>();
    }

    /**
     * 加载图片
     *
     * @param url
     * @param imageView
     */
    public void loadImage(final String url, final ImageView imageView) {
        if (mConfig == null) {
            throw new RuntimeException(
                    "The config of SimpleImageLoader is Null, please call the init(ImageLoaderConfig config) method to initialize");
        }
        loadImage(url, imageView, null);
    }

    public void loadImage(final String url, final ImageView imageView, DisplayConfig displayConfig){
        if (mConfig == null) {
            throw new RuntimeException(
                    "The config of SimpleImageLoader is Null, please call the init(ImageLoaderConfig config) method to initialize");
        }

        // set tag
        imageView.setTag(url);

        final String key = hashKeyFormUrl(url);
        ImageSize imageSize = getImageViewWidth(imageView);
        Bitmap bitmap = mImageCache.get(key, imageSize.width, imageSize.height);

        if (bitmap != null) {
            ImgBeanHolder holder = new ImgBeanHolder(bitmap, imageView, url);
            Message message = Message.obtain();
            message.obj = holder;
            mUiHandler.sendMessage(message);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = downloadBitmapFromUrl(url);
                    if (bitmap != null) {
                        ImgBeanHolder holder = new ImgBeanHolder(bitmap, imageView, url);
                        Message message = Message.obtain();
                        message.obj = holder;
                        mUiHandler.sendMessage(message);

                        mImageCache.put(key, bitmap);
                    }

                    mPoolSemaphore.release();
                }
            });
        }
    }

    private Bitmap downloadBitmapFromUrl(String urlString) {
        Bitmap bitmap = null;
        HttpURLConnection conn = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(conn.getInputStream());
            bitmap = BitmapFactory.decodeStream(in);
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmap: " + e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }

            IOUtil.close(in);
        }
        return bitmap;
    }

    private String hashKeyFormUrl(String url) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 添加一个任务.
     *
     * @param runnable
     */
    private synchronized void addTask(Runnable runnable) {
        try {
            // 请求信号量，防止mPoolThreadHander为null
            if (mPoolThreadHander == null)
                mSemaphore.acquire();
        } catch (InterruptedException e) {
        }
        mTasks.add(runnable);
        mPoolThreadHander.sendEmptyMessage(0x110);
    }

    /**
     * 取出一个任务.
     *
     * @return
     */
    private synchronized Runnable getTask() {
        if (mConfig.type == Type.FIFO) {
            return mTasks.removeFirst();
        } else if (mConfig.type == Type.LIFO) {
            return mTasks.removeLast();
        }
        return null;
    }


    /**
     * 根据ImageView获得适当的压缩的宽和高
     *
     * @param imageView
     * @return
     */
    private ImageSize getImageViewWidth(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        final DisplayMetrics displayMetrics = imageView.getContext()
                .getResources().getDisplayMetrics();
        final LayoutParams params = imageView.getLayoutParams();

        int width = params.width == LayoutParams.WRAP_CONTENT ? 0 : imageView
                .getWidth(); // Get actual image width
        if (width <= 0)
            width = params.width; // Get layout width parameter
        if (width <= 0)
            width = getImageViewFieldValue(imageView, "mMaxWidth"); // Check
        // maxWidth
        // parameter
        if (width <= 0)
            width = displayMetrics.widthPixels;


        int height = params.height == LayoutParams.WRAP_CONTENT ? 0 : imageView
                .getHeight(); // Get actual image height
        if (height <= 0)
            height = params.height; // Get layout height parameter
        if (height <= 0)
            height = getImageViewFieldValue(imageView, "mMaxHeight"); // Check
        // maxHeight
        // parameter
        if (height <= 0)
            height = displayMetrics.heightPixels;
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;

    }


    private class ImgBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;

        ImgBeanHolder(Bitmap bitmap, ImageView imageView, String path) {
            this.bitmap = bitmap;
            this.imageView = imageView;
            this.path = path;
        }
    }

    private class ImageSize {
        int width;
        int height;
    }

    /**
     * 反射获得ImageView设置的最大宽度和高度.
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = (Integer) field.get(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;

                Log.e("TAG", value + "");
            }
        } catch (Exception e) {
        }
        return value;
    }
}
