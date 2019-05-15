package com.ysbing.samples.glint.http;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ysbing.glint.base.GlintResultBean;
import com.ysbing.glint.http.GlintHttp;
import com.ysbing.glint.http.GlintHttpListener;
import com.ysbing.samples.glint.R;

import java.util.TreeMap;

import okhttp3.Headers;

/**
 * Created by chenzhujie on 2019/5/13
 */
public class HttpModuleRequestActivity extends AppCompatActivity {

    private TextView mTextView;

    public static void startAction(Activity activity) {
        Intent intent = new Intent(activity, HttpModuleRequestActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_request);
        Button button = findViewById(R.id.button);
        mTextView = findViewById(R.id.content);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request();
            }
        });
    }

    private void request() {
        String url = "https://www.sojson.com/open/api/lunar/json.shtml";
        TreeMap<String, String> params = new TreeMap<>();
        params.put("date", "2019-05-01");
        Headers.Builder headers = new Headers.Builder();
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
        GlintHttp.get(url, params).setHeader(headers)
                .using(MyHttpModule.get()).execute(new GlintHttpListener<LunarBean>() {
            @Override
            public void onStart() {
                super.onStart();
                String str = "->onStart()";
                mTextView.setText(str);
            }

            @Override
            public void onResponse(@NonNull GlintResultBean<LunarBean> resultBean) throws Throwable {
                super.onResponse(resultBean);
                String str = mTextView.getText() + "\n\n->onResponse()," + resultBean.getResponseStr();
                mTextView.setText(str);
            }

            @Override
            public void onSuccess(@NonNull LunarBean result) throws Throwable {
                super.onSuccess(result);
                String str = mTextView.getText() + "\n\n->onSuccess()," + result;
                mTextView.setText(str);
            }

            @Override
            public void onFail(@NonNull Throwable error) {
                super.onFail(error);
                String str = mTextView.getText() + "\n\n->onFail()," + error.toString();
                mTextView.setText(str);
            }

            @Override
            public void onError(int status, @NonNull String errMsg) throws Throwable {
                super.onError(status, errMsg);
                String str = mTextView.getText() + "\n\n->onError(),status:" + status + ",errMsg:" + errMsg;
                mTextView.setText(str);
            }

            @Override
            public void onFinish() {
                super.onFinish();
                String str = mTextView.getText() + "\n\n->onFinish()";
                mTextView.setText(str);
            }
        });
    }
}
