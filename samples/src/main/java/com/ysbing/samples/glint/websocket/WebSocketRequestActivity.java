package com.ysbing.samples.glint.websocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ysbing.glint.socket.GlintSocket;
import com.ysbing.glint.socket.GlintSocketListener;
import com.ysbing.samples.glint.R;

/**
 * Created by chenzhujie on 2019/5/14
 */
public class WebSocketRequestActivity extends AppCompatActivity {
    private static final String SOCKET_URL = "http://socket.test";
    private static final String SOCKET_CMD_ON = "on_cmd";
    private static final String SOCKET_CMD_SEND = "msg_cmd";
    private EditText mEditTextContent;
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
        mEditTextContent = findViewById(R.id.et_content);
        buttonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offLastSocket();
                socketOn();
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offLastSocket();
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

    private void offLastSocket() {
        GlintSocket.off(SOCKET_URL, SOCKET_CMD_ON);
        GlintSocket.off(SOCKET_URL, SOCKET_CMD_SEND);
    }

    private void socketOn() {
        GlintSocket.on(SOCKET_URL, SOCKET_CMD_ON).execute(new GlintSocketListener<String>() {
            @Override
            public void onProcess(@NonNull String result) throws Exception {
                super.onProcess(result);
                text += "socketOn,result:" + result + "\n\n";
                updateEditText();
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
                text += "socketOn,error:" + error + "\n\n";
                updateEditText();
            }
        });
    }

    private void socketSend() {
        GlintSocket.send(SOCKET_URL, SOCKET_CMD_SEND, "我是消息").execute(new GlintSocketListener<String>() {
            @Override
            public void onProcess(@NonNull String result) throws Exception {
                super.onProcess(result);
                text += "socketSend,result:" + result + "\n\n";
                updateEditText();
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
                text += "socketSend,error:" + error + "\n\n";
                updateEditText();
            }
        });
    }

    private void updateEditText() {
        mEditTextContent.setText(text);
        mEditTextContent.setSelection(text.length());
    }
}

