package com.ysbing.samples.glint.websocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ysbing.glint.socket.GlintSocket;
import com.ysbing.glint.socket.GlintSocketListener;
import com.ysbing.samples.glint.R;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chenzhujie on 2019/5/14
 */
public class WebSocketRequestActivity extends AppCompatActivity {
    private static final String SOCKET_URL = "ws://echo.websocket.org";
    private final AtomicInteger msgId = new AtomicInteger();
    private TextView mTextView;
    private String text = "";

    public static void startAction(Activity activity) {
        Intent intent = new Intent(activity, WebSocketRequestActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_websocket_request);
        Button buttonOn = findViewById(R.id.btn_on);
        Button buttonSend = findViewById(R.id.btn_send);
        mTextView = findViewById(R.id.et_content);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        buttonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlintSocket.off(SOCKET_URL, GlintSocket.EVENT_CONNECT);
                GlintSocket.off(SOCKET_URL, MySocketHttpModule.SOCKET_CMD_SEND);
                socketOn();
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socketSend();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            GlintSocket.off(SOCKET_URL);
        }
    }

    private void socketOn() {
        GlintSocket.on(SOCKET_URL, GlintSocket.EVENT_CONNECT).execute(new GlintSocketListener<String>() {
            @Override
            public void onProcess(@NonNull String result) throws Throwable {
                super.onProcess(result);
                text = "socketOn,EVENT_CONNECT\n\n" + text;
                updateEditText();
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
                text = "socketOn,EVENT_CONNECT,error:" + error + "\n\n" + text;
                updateEditText();
            }
        });
        GlintSocket.on(SOCKET_URL, MySocketHttpModule.SOCKET_CMD_SEND)
                .using(MySocketHttpModule.get()).execute(new GlintSocketListener<String>() {
            @Override
            public void onProcess(@NonNull String result) throws Throwable {
                super.onProcess(result);
                text = "socketOn," + MySocketHttpModule.SOCKET_CMD_SEND + ",result:" + result + "\n\n" + text;
                updateEditText();
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
                text = "socketOn," + MySocketHttpModule.SOCKET_CMD_SEND + ",error:" + error + "\n\n" + text;
                updateEditText();
            }
        });
    }

    private void socketSend() {
        GlintSocket.send(SOCKET_URL, "我是消息" + msgId.incrementAndGet()).execute();
    }

    private void updateEditText() {
        mTextView.setText(text);
    }
}

