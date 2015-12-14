类似ImageLoader的图片加载框架，使用LruCache做内存缓存，使用DiskLruCache做sd卡缓存，目前只支持网络图片的加载。

1. 使用线程池加载网络图片。
2. 基于“单一职责原则” 分离逻辑跟cache部分。
3. 基于“开闭原则” 抽象出cache接口，便于扩展任意类型的缓存策略。
4. 使用Build设计模式实现Imageloader的配置。

使用方法：
        
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration().setmImageCache(new DoubleCache(this)).setmType(ImageLoader.Type.LIFO).setThreadCount(3);
        
        ImageLoader.getInstance().init(this, configuration);
         
        ImageLoader.getInstance().loadImage(uri, imageView);
