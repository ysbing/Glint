package com.ysbing.samples.glint.upload;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ysbing.glint.upload.GlintUpload;
import com.ysbing.glint.upload.GlintUploadListener;
import com.ysbing.samples.glint.R;
import com.ysbing.ypermission.PermissionManager;

import java.io.File;

/**
 * Created by chenzhujie on 2019/5/14
 */
public class UploadRequestActivity extends AppCompatActivity {

    public static final int SELECT_PHOTO = 1;
    private TextView mTextViewStatus;
    private ImageView mImageView;
    private String path;

    public static void startAction(Activity activity) {
        Intent intent = new Intent(activity, UploadRequestActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_request);
        Button buttonSelect = findViewById(R.id.btn_select);
        Button buttonUpload = findViewById(R.id.btn_upload);
        mTextViewStatus = findViewById(R.id.tv_status);
        mImageView = findViewById(R.id.iv_image);
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                PermissionManager.requestPermission(UploadRequestActivity.this, permissions, new PermissionManager.PermissionsListener() {
                    @Override
                    public void onPermissionGranted() {
                        pickPhoto();
                    }
                });
            }
        });
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request();
            }
        });
    }

    /***
     * 从相册中选取图片
     */
    public void pickPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose Image..."), SELECT_PHOTO);
    }

    private void request() {
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show();
        } else {
            String url = "http://google.com/upload.do";
            GlintUpload.upload(url, new File(path)).execute(new GlintUploadListener<String>() {
                @Override
                public void onStart() {
                    super.onStart();
                    String str = "onStart()";
                    mTextViewStatus.setText(str);
                }

                @Override
                public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
                    super.onProgress(bytesWritten, contentLength, speed, percent);
                    String str = "onProgress(),percent:" + percent + "%";
                    mTextViewStatus.setText(str);
                }

                @Override
                public void onSuccess(@NonNull String result) throws Throwable {
                    super.onSuccess(result);
                    String str = "onSuccess()," + result;
                    mTextViewStatus.setText(str);
                }

                @Override
                public void onFail(@NonNull Throwable error) {
                    super.onFail(error);
                    String str = "onFail()," + error.toString();
                    mTextViewStatus.setText(str);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PHOTO) {
            if (data != null) {
                path = ContentUriUtil.getPath(this, data.getData());
                mImageView.setImageURI(data.getData());
            }
        }
    }
}
