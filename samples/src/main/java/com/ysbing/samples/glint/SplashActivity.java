package com.ysbing.samples.glint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("Glint");
        textView.setTextColor(getResources().getColor(android.R.color.white));
        textView.setTextSize(50f);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(Color.parseColor("#70C360"));
        setContentView(textView);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 1000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}