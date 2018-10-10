
中文版本请参看[这里](./readme_cn.md)

## I. Introduction

Glint is an Http standard protocol framework based on OkHttp for Android. It supports four functions: interface request, file download, file upload and WebSocket.

## II. Feature
* The underlying framework is OkHttp
* Support asynchronous request, support synchronous request
* Support file upload
* Support file download, support pause and resume, resume download
* Support long connection, compatible with grapefruit IO
* Failure retry mechanism
* Intelligent life cycle, avoiding memory leaks after leaving Activity or Fragment
* Custom HttpModule for custom parsing request results and adding public parameters
* Support cancels a request, cancels all requests
* The entrance is easy to remember, which are GlintHttp, GlintDownload, GlintUpload, GlintSocket

## III. Install

Maven is recommended:
``` gradle
Dependencies {
    Implementation 'com.ysbing.glint:glint:1.0.0'
    // replace "1.0.10" with any available version
}
```

## IV. HTTP request

### Request queue
The most basic request:

``` java
String url = "https://www.sojson.com/open/api/lunar/json.shtml?date=2017-05-27";
GlintHttp.get(url).execute(new GlintHttpListener<String>() {
    @Override
    Public void onSuccess(@NonNull String result) throws Exception {
        super.onSuccess(result);
    }
});
```

### Advanced Custom Configuration

Create a new class, inherit BaseHttpModule, use HttpModule has two ways to use

The first way: declare the Module as a generic module of the Application in the AndroidManifest file, it should be noted that each Application can only have a common Module.

``` xml
<meta-data
 Android:name="com.ysbing.samples.glint.MyHttpModule"
 Android:value="GlintHttpModule"
 />
```


 The second way: When using a separate request, use the using method to pass the Module.

 If neither of the above methods is used, the standard json will be parsed into the corresponding entity class.

 The HttpModule class that uses the first method must be public so that they can be instantiated by reflection when GlintHttp is lazy initialized.

Request with configuration, using the using method

``` java
String url = "https://www.sojson.com/open/api/lunar/json.shtml";
TreeMap<String, String> params = new TreeMap<>();
Params.put("date", "2018-10-01");
GlintHttp.get(url, params).using(MyHttpModule.get()).execute(new GlintHttpListener<LunarBean>() {
    @Override
    Public void onSuccess(@NonNull LunarBean result) throws Exception {
        super.onSuccess(result);
    }

    @Override
    Public void onFail(@NonNull Throwable error) {
        super.onFail(error);
    }

    @Override
    Public void onFinish() {
        super.onFinish();
    }
});
```

For other usage methods, please refer to the public method of GlintHttp.

### HTTP request to add parameters
The parameter uses TreeMap to load the data. When using, do not pass in the null key.

### OkHttpConfiguration
Override the onOkHttpBuildCreate method in BaseHttpModule

``` java
@Override
Public OkHttpClient.Builder onOkHttpBuildCreate(@NonNull Glint.GlintType clientType, @NonNull OkHttpClient.Builder builder) {
    Return builder.readTimeout(3000L, TimeUnit.MILLISECONDS)
            .writeTimeout(5000L, TimeUnit.MILLISECONDS);
}
```

### GlintHttpModule Configuration

``` java
Public class MyHttpModule extends BaseHttpModule {

    Public static MyHttpModule get() {
        Return new MyHttpModule();
    }

    @Override
    Public OkHttpClient.Builder onOkHttpBuildCreate(@NonNull Glint.GlintType clientType, @NonNull OkHttpClient.Builder builder) {
        Return builder.readTimeout(3000L, TimeUnit.MILLISECONDS)
                .writeTimeout(5000L, TimeUnit.MILLISECONDS);
    }

    @Override
    Public <E extends BaseHttpModule> void configDefaultBuilder(@NonNull GlintBaseBuilder<E> builder) {
        super.configDefaultBuilder(builder);
    }

    @Override
    Public boolean getHeaders(@NonNull Map<String, String> originalHeader) throws Exception {
        Return super.getHeaders(originalHeader);
    }

    @Override
    Public boolean getParams(@NonNull TreeMap<String, String> originalParams) throws Exception {
        Return super.getParams(originalParams);
    }

    @Override
    Public boolean getParams(@NonNull TreeMap<String, String> originalParams, @Nullable JsonObject originalJsonParams) throws Exception {
        Return super.getParams(originalParams, originalJsonParams);
    }

    @Override
    Public UrlResult getUrl(@NonNull String originalUrl) throws Exception {
        Return super.getUrl(originalUrl);
    }

    @Override
    Public <T> boolean customDeserialize(@NonNull GlintResultBean<T> result, @NonNull JsonObject jsonObj, @NonNull Gson gson, @NonNull Type typeOfT) throws Exception {
        JsonElement statusElement = jsonObj.get("status");
        JsonElement messageElement = jsonObj.get("message");
        JsonElement dataElement = jsonObj.get("data");
        / / If it is empty, it may be the standard json, without judging the status code, directly parsing
        If (statusElement == null) {
            result.setRunStatus(Glint.ResultStatus.STATUS_NORMAL);
            result.setData(GlintRequestUtil.<T>standardDeserialize(gson, jsonObj, typeOfT));
        } else {
            // status node, here to determine whether the request is successful
            Int status = statusElement.getAsInt();
            If (messageElement != null) {
                // message node
                String message = messageElement.getAsString();
                result.setMessage(message);
            }
            result.setStatus(status);
            If (status == 200) {
                result.setRunStatus(Glint.ResultStatus.STATUS_SUCCESS);
                If (dataElement != null) {
                    result.setData(GlintRequestUtil.<T>successDeserialize(gson, dataElement, typeOfT));
                }
            } else {
                result.setRunStatus(Glint.ResultStatus.STATUS_ERROR);
            }
        }
        Return false;
    }
}
```
This is all configured, if you do not need this advanced configuration, you can use it without any configuration. In this configuration, the onOkHttpBuildCreate method is only executed once in this process, and other methods are executed in each request.


## V. File download request
The following is the basic method of use

``` java
GlintDownload.download("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk", new File(getExternalCacheDir(), "mobileqq_android.apk")).execute(new GlintDownloadListener() {
    @Override
    Public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
        super.onProgress(bytesWritten, contentLength, speed, percent);
    }

    @Override
    Public void onSuccess(@NonNull File result) throws Exception {
        super.onSuccess(result);
    }
});
```
More advanced usage can be found in the source code, such as the download process support can be canceled, can be suspended and resumed.

## VI. File upload request

Here's the basic usage:

```
GlintUpload.upload("https://www.qq.com/", new File(getExternalCacheDir(), "mobileqq_android.apk")).execute(new GlintUploadListener<String>() {
    @Override
    Public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
        super.onProgress(bytesWritten, contentLength, speed, percent);
    }

    @Override
    Public void onSuccess(@NonNull String result) throws Exception {
        super.onSuccess(result);
    }
});
```

## VII. WebSocket request

### The following is how to send a message:

``` java
GlintSocket.sendIO(url, "cmd", "I am a message").execute(new GlintSocketListener<String>() {
    @Override
    Public void onProcess(@NonNull String result) throws Exception {
        super.onProcess(result);
    }

    @Override
    Public void onError(@NonNull String error) {
        super.onError(error);
    }
});
```

Note that GlintSocketRequest has send and sendIO and two methods. If you connect to WebSocket, use send. If you connect grapefruit IO, use sendIO.

### The following is how to set up an event listener:

``` java
GlintSocket.on("http://socket.test", "cmd").execute(new GlintSocketListener<String>() {
    @Override
    Public void onProcess(@NonNull String result) throws Exception {
        super.onProcess(result);
    }

    @Override
    Public void onError(@NonNull String error) {
        super.onError(error);
    }
});
```
When you don't need a Socket, remember to release the connection. Otherwise, the memory will leak. The release method is as follows:

``` java
GlintSocket.off("http://socket.test", "cmd");
```

## VIII. Tools

### Easily get a Contex object to get the Application object:

``` java
Context context = ContextHelper.getAppContext();
Application application = ContextHelper.getApplication();
```
 
### Get the MD5 value of a string or file:

``` java
String strMd5 = Md5Util.getMD5Str("hello, world");
String fileMd5 = Md5Util.getMD5Str(new File("demo.txt"));
```
 
### Quickly cut from the child thread to the main thread:

``` java
UiKit.runOnMainThreadAsync(new Runnable() {
    @Override
    Public void run() {
 
    }
});
UiKit.runOnMainThreadSync(new Runnable() {
    @Override
    Public void run() {
 
    }
});
UiKit.runOnMainThreadSync(new Runnable() {
    @Override
    Public void run() {
 
    }
}, 1000, false);
```