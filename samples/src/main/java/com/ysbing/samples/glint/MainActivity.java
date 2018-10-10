package com.ysbing.samples.glint;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.ysbing.glint.download.GlintDownload;
import com.ysbing.glint.download.GlintDownloadListener;
import com.ysbing.glint.http.GlintHttp;
import com.ysbing.glint.http.GlintHttpListener;
import com.ysbing.glint.socket.GlintSocket;
import com.ysbing.glint.socket.GlintSocketListener;
import com.ysbing.glint.upload.GlintUpload;
import com.ysbing.glint.upload.GlintUploadListener;

import java.io.File;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String url = "https://www.sojson.com/open/api/lunar/json.shtml?date=2017-05-27";
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
        GlintSocket.sendIO("http://socket.test", "cmd", "我是消息").execute(new GlintSocketListener<String>() {
            @Override
            public void onProcess(@NonNull String result) throws Exception {
                super.onProcess(result);
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
            }
        });
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
        GlintSocket.off("http://socket.test", "cmd");
    }
}
