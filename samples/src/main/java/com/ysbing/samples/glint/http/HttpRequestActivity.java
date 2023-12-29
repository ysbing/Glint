package com.ysbing.samples.glint.http;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ysbing.glint.http.GlintHttp;
import com.ysbing.glint.http.GlintHttpListener;
import com.ysbing.samples.glint.R;

/**
 * Created by chenzhujie on 2019/5/14
 */
public class HttpRequestActivity extends AppCompatActivity {

    private TextView mTextView;

    public static void startAction(Activity activity) {
        Intent intent = new Intent(activity, HttpRequestActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_request);
        Button button = findViewById(R.id.button);
        mTextView = findViewById(R.id.content);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request();
            }
        });
    }

    private void request() {
        String url = "https://www.baidu.com/";
        GlintHttp.get(url).notJson(true).execute(new GlintHttpListener<String>() {
            @Override
            public void onSuccess(@NonNull String result) throws Throwable {
                super.onSuccess(result);
                mTextView.setText(result);
            }
        });
    }
}
