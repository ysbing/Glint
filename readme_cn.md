# 一、简介

Glint是Android实现基于OkHttp的Http标准协议框架，支持接口请求，文件下载，文件上传，WebSocket四大功能。 

# 二、框架特性 
* 底层框架为OkHttp
* 支持异步请求、支持同步请求
* 支持文件上传
* 支持文件下载，支持暂停和恢复，断点续传
* 支持长连接，兼容柚子IO
* 失败重试机制
* 智能生命周期，避免离开Activity或Fragment后导致内存泄露
* 自定义HttpModule，用于自定义解析请求结果和添加公共参数
* 支持取消某个请求、取消所有请求
* 入口好记，分别是GlintHttp，GlintDownload，GlintUpload，GlintSocket

# 三、使用方法

推荐使用 Maven：
``` gradle
dependencies {
    implementation 'com.ysbing.glint:glint:1.0.0'
    // replace "1.0.10" with any available version
}
```

# 四、HTTP请求 

## 请求队列
最基本的请求：

``` java
String url = "https://www.sojson.com/open/api/lunar/json.shtml?date=2017-05-27";
GlintHttp.get(url).execute(new GlintHttpListener<String>() {
    @Override
    public void onSuccess(@NonNull String result) throws Exception {
        super.onSuccess(result);
    }
});
```

## 高级自定义配置 

新建一个类，继承BaseHttpModule，使用HttpModule有两种使用方式

第一种方式：在AndroidManifest文件中声明该Module作为该Application通用的Module，需注意的是，每一个Application只能拥有一个通用的Module。

``` xml
<meta-data
 android:name="com.ysbing.samples.glint.MyHttpModule"
 android:value="GlintHttpModule"
 />
```


 第二种方式：在单独请求的时候，使用using方法，将该Module传入。

 如果以上两种方式都不使用的话，将采取标准的json解析到对应的实体类。

 使用第一种方式的HttpModule类必须是public的，以便在GlintHttp延迟初始化时，可以通过反射将它们实例化。

带有配置的请求，使用using方法

``` java
String url = "https://www.sojson.com/open/api/lunar/json.shtml";
TreeMap<String, String> params = new TreeMap<>();
params.put("date", "2018-10-01");
GlintHttp.get(url, params).using(MyHttpModule.get()).execute(new GlintHttpListener<LunarBean>() {
    @Override
    public void onSuccess(@NonNull LunarBean result) throws Exception {
        super.onSuccess(result);
    }

    @Override
    public void onFail(@NonNull Throwable error) {
        super.onFail(error);
    }

    @Override
    public void onFinish() {
        super.onFinish();
    }
});
```

其他使用方法请查阅GlintHttp的公开方法。 

## HTTP请求添加参数
参数使用TreeMap来装载数据，使用时，请勿传入null键

## OkHttp配置
复写BaseHttpModule里的onOkHttpBuildCreate方法

``` java
@Override
public OkHttpClient.Builder onOkHttpBuildCreate(@NonNull Glint.GlintType clientType, @NonNull OkHttpClient.Builder builder) {
    return builder.readTimeout(3000L, TimeUnit.MILLISECONDS)
            .writeTimeout(5000L, TimeUnit.MILLISECONDS);
}
```

## GlintHttpModule配置

``` java
public class MyHttpModule extends BaseHttpModule {

    public static MyHttpModule get() {
        return new MyHttpModule();
    }

    @Override
    public OkHttpClient.Builder onOkHttpBuildCreate(@NonNull Glint.GlintType clientType, @NonNull OkHttpClient.Builder builder) {
        return builder.readTimeout(3000L, TimeUnit.MILLISECONDS)
                .writeTimeout(5000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public <E extends BaseHttpModule> void configDefaultBuilder(@NonNull GlintBaseBuilder<E> builder) {
        super.configDefaultBuilder(builder);
    }

    @Override
    public boolean getHeaders(@NonNull Map<String, String> originalHeader) throws Exception {
        return super.getHeaders(originalHeader);
    }

    @Override
    public boolean getParams(@NonNull TreeMap<String, String> originalParams) throws Exception {
        return super.getParams(originalParams);
    }

    @Override
    public boolean getParams(@NonNull TreeMap<String, String> originalParams, @Nullable JsonObject originalJsonParams) throws Exception {
        return super.getParams(originalParams, originalJsonParams);
    }

    @Override
    public UrlResult getUrl(@NonNull String originalUrl) throws Exception {
        return super.getUrl(originalUrl);
    }

    @Override
    public <T> boolean customDeserialize(@NonNull GlintResultBean<T> result, @NonNull JsonObject jsonObj, @NonNull Gson gson, @NonNull Type typeOfT) throws Exception {
        JsonElement statusElement = jsonObj.get("status");
        JsonElement messageElement = jsonObj.get("message");
        JsonElement dataElement = jsonObj.get("data");
        // 如果为空，可能是标准的json，不用判断状态码，直接解析
        if (statusElement == null) {
            result.setRunStatus(Glint.ResultStatus.STATUS_NORMAL);
            result.setData(GlintRequestUtil.<T>standardDeserialize(gson, jsonObj, typeOfT));
        } else {
            // status节点，这里判断出是否请求成功
            int status = statusElement.getAsInt();
            if (messageElement != null) {
                // message节点
                String message = messageElement.getAsString();
                result.setMessage(message);
            }
            result.setStatus(status);
            if (status == 200) {
                result.setRunStatus(Glint.ResultStatus.STATUS_SUCCESS);
                if (dataElement != null) {
                    result.setData(GlintRequestUtil.<T>successDeserialize(gson, dataElement, typeOfT));
                }
            } else {
                result.setRunStatus(Glint.ResultStatus.STATUS_ERROR);
            }
        }
        return false;
    }
}
```
这样就全部配置完毕了，如果你不需要这个高级配置的话，可无需任何配置也可以正常使用，在这份配置中，onOkHttpBuildCreate方法在本进程只执行一次，其他方法在每个请求都会执行。


# 五、文件下载请求

下面是基本的使用方法

``` java
GlintDownload.download("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk", new File(getExternalCacheDir(), "mobileqq_android.apk")).execute(new GlintDownloadListener() {
    @Override
    public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
        super.onProgress(bytesWritten, contentLength, speed, percent);
    }

    @Override
    public void onSuccess(@NonNull File result) throws Exception {
        super.onSuccess(result);
    }
});
```
更多高级用法可参阅源码，如下载过程支持可取消，可暂停和恢复。

# 六、文件上传请求

下面是基本的使用方法：

```
GlintUpload.upload("https://www.qq.com/", new File(getExternalCacheDir(), "mobileqq_android.apk")).execute(new GlintUploadListener<String>() {
    @Override
    public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
        super.onProgress(bytesWritten, contentLength, speed, percent);
    }

    @Override
    public void onSuccess(@NonNull String result) throws Exception {
        super.onSuccess(result);
    }
});
```

# 七、WebSocket请求

## 下面是发送一条消息的使用方法：

``` java
GlintSocket.sendIO(url, "cmd", "我是消息").execute(new GlintSocketListener<String>() {
    @Override
    public void onProcess(@NonNull String result) throws Exception {
        super.onProcess(result);
    }

    @Override
    public void onError(@NonNull String error) {
        super.onError(error);
    }
});
```

注意，GlintSocketRequest有send和sendIO和两个方法，如果连接的是WebSocket就使用send，连接的是柚子IO就使用sendIO

## 下面是设置一个事件监听的使用方法：

``` java
GlintSocket.on("http://socket.test", "cmd").execute(new GlintSocketListener<String>() {
    @Override
    public void onProcess(@NonNull String result) throws Exception {
        super.onProcess(result);
    }

    @Override
    public void onError(@NonNull String error) {
        super.onError(error);
    }
});
```
当不需要Socket的时候，记住要将该连接做释放处理，否则会导致内存泄露，释放的使用方法如下：

``` java
GlintSocket.off("http://socket.test", "cmd");
```

# 八、常用工具类

## 轻松获取一个Contex对象获取Application对象：

``` java
Context context = ContextHelper.getAppContext();
Application application = ContextHelper.getApplication();
```
 
## 获取字符串或文件的MD5值：

``` java
String strMd5 = Md5Util.getMD5Str("hello, world");
String fileMd5 = Md5Util.getMD5Str(new File("demo.txt"));
```
 
## 从子线程快速切到主线程：

``` java
UiKit.runOnMainThreadAsync(new Runnable() {
    @Override
    public void run() {
 
    }
});
UiKit.runOnMainThreadSync(new Runnable() {
    @Override
    public void run() {
 
    }
});
UiKit.runOnMainThreadSync(new Runnable() {
    @Override
    public void run() {
 
    }
}, 1000, false);
```