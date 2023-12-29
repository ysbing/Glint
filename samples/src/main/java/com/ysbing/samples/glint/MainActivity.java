package com.ysbing.samples.glint;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ysbing.samples.glint.download.DownloadRequestActivity;
import com.ysbing.samples.glint.http.HttpModuleRequestActivity;
import com.ysbing.samples.glint.http.HttpRequestActivity;
import com.ysbing.samples.glint.upload.UploadRequestActivity;
import com.ysbing.samples.glint.websocket.WebSocketRequestActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static class MyViewHolder extends RecyclerView.ViewHolder {
        private MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class MyData {
        private final String title;
        private final String des;

        private MyData(String title, String des) {
            this.title = title;
            this.des = des;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new RecyclerView.Adapter<MyViewHolder>() {
            private final List<MyData> data = new ArrayList<MyData>() {{
                add(new MyData("普通请求", "请求一个HTML页面"));
                add(new MyData("复杂请求", "自定义Module，可对OkHttp加拦截器和自定义解析数据等配置"));
                add(new MyData("上传请求", "将本地文件上传到服务器"));
                add(new MyData("下载请求", "下载文件到本地"));
                add(new MyData("WebSocket请求", "监听Socket事件"));
            }};

            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View itemView = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.item_list, viewGroup, false);
                return new MyViewHolder(itemView);
            }

            @Override
            public void onBindViewHolder(@NonNull final MyViewHolder myViewHolder, int i) {
                TextView titleView = myViewHolder.itemView.findViewById(R.id.tv_title);
                TextView contentView = myViewHolder.itemView.findViewById(R.id.tv_content);
                titleView.setText(data.get(i).title);
                contentView.setText(data.get(i).des);
                myViewHolder.itemView.setOnClickListener(v -> {
                    switch (myViewHolder.getBindingAdapterPosition()) {
                        case 0:
                            HttpRequestActivity.startAction(MainActivity.this);
                            break;
                        case 1:
                            HttpModuleRequestActivity.startAction(MainActivity.this);
                            break;
                        case 2:
                            UploadRequestActivity.startAction(MainActivity.this);
                            break;
                        case 3:
                            DownloadRequestActivity.startAction(MainActivity.this);
                            break;
                        case 4:
                            WebSocketRequestActivity.startAction(MainActivity.this);
                            break;
                        default:
                            break;
                    }
                });
            }

            @Override
            public int getItemCount() {
                return data.size();
            }
        });
    }
}
