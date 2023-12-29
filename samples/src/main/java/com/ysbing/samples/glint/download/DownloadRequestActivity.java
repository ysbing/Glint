package com.ysbing.samples.glint.download;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ysbing.glint.download.GlintDownload;
import com.ysbing.glint.download.GlintDownloadListener;
import com.ysbing.samples.glint.R;

import java.io.File;

import okhttp3.Response;

/**
 * Created by chenzhujie on 2019/5/14
 */
public class DownloadRequestActivity extends AppCompatActivity {
    private Button mButtonDownload;
    private TextView mTextViewStatus;
    private ProgressBar mProgressBar;
    private GlintDownload mGlintDownload;
    private boolean isDownload;

    public static void startAction(Activity activity) {
        Intent intent = new Intent(activity, DownloadRequestActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_request);
        mButtonDownload = findViewById(R.id.btn_download);
        mTextViewStatus = findViewById(R.id.tv_status);
        mProgressBar = findViewById(R.id.progress_bar);
        mButtonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGlintDownload != null) {
                    if (isDownload) {
                        mGlintDownload.pause();
                    } else {
                        mGlintDownload.resume();
                    }
                } else {
                    request();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGlintDownload != null) {
            mGlintDownload.cancel();
        }
    }

    private void request() {
        String url = "https://download.sj.qq.com/upload/connAssitantDownload/upload/MobileAssistant_1.apk";
        File savePath = getExternalFilesDir("download");
        if (savePath == null) {
            savePath = getFilesDir();
        }
        mGlintDownload = GlintDownload.download(url, savePath);
        mGlintDownload.execute(new GlintDownloadListener() {
            @Override
            public void onStart() {
                super.onStart();
                String str = "onStart()";
                mTextViewStatus.setText(str);
                isDownload = true;
                mButtonDownload.setText("暂停");
            }

            @Override
            public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
                super.onProgress(bytesWritten, contentLength, speed, percent);
                String str = percent + "%,onProgress()";
                mTextViewStatus.setText(str);
                mProgressBar.setProgress(percent);
            }

            @Override
            public void onPause() {
                super.onPause();
                String str = "onPause()";
                mTextViewStatus.setText(str);
                mButtonDownload.setText("继续");
                isDownload = false;
            }

            @Override
            public void onResume() {
                super.onResume();
                String str = "onResume()";
                mTextViewStatus.setText(str);
                mButtonDownload.setText("暂停");
                isDownload = true;
            }

            @Override
            public void onCancel() {
                super.onCancel();
                String str = "onCancel()";
                mTextViewStatus.setText(str);
                mButtonDownload.setText("开始");
                isDownload = false;
            }

            @Override
            public void onDownloadFail(@NonNull Throwable error, @Nullable Response response) {
                super.onDownloadFail(error, response);
                String str = "onDownloadFail(),error:" + error;
                mTextViewStatus.setText(str);
                mButtonDownload.setText("继续");
                isDownload = false;
            }

            @Override
            public void onSuccess(@NonNull File result) throws Throwable {
                super.onSuccess(result);
                String str = "onSuccess(),file:" + result;
                mTextViewStatus.setText(str);
            }

            @Override
            public void onFinish() {
                super.onFinish();
                isDownload = false;
                mGlintDownload = null;
                mButtonDownload.setText("开始");
            }
        });
    }
}
