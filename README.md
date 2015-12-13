这是一个仿ImageLoader实现的图片加载框架，使用LruCache做内存缓存，使用DiskLruCache做sd卡缓存，目前只能加载网络图片，会持续更新。

1. 使用线程池加载网络图片。
2. 基于“单一职责原则” 分离了逻辑跟cache部分。
3. 基于“开闭原则” 抽象出缓存接口，便于扩展任意类型的缓存策略。
4. 使用Build设计模式实现Imageloader的配置。

使用方法：
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration().setmImageCache(new DoubleCache(this)).setmType(ImageLoader.Type.LIFO).setThreadCount(3);
        ImageLoader.getInstance().init(this, configuration);
          ImageLoader.getInstance().loadImage(uri, imageView);
