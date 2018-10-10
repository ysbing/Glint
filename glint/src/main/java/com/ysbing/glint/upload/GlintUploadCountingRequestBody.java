package com.ysbing.glint.upload;

import android.support.annotation.NonNull;

import com.ysbing.glint.util.UiKit;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * 上传读文件操作
 *
 * @author ysbing
 *         创建于 2018/1/16
 */
public final class GlintUploadCountingRequestBody extends RequestBody {

    private final RequestBody body;
    private final GlintUploadListener listener;
    private long lastRefreshTime;
    private long lastBytesWritten;
    private long speed;

    GlintUploadCountingRequestBody(RequestBody body, GlintUploadListener listener) {
        this.body = body;
        this.listener = listener;
    }

    @Override
    public long contentLength() throws IOException {
        return body.contentLength();
    }

    @Override
    public MediaType contentType() {
        return body.contentType();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        BufferedSink bufferedSink;
        CountingSink countingSink = new CountingSink(sink);
        bufferedSink = Okio.buffer(countingSink);
        body.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    protected final class CountingSink extends ForwardingSink {

        private long bytesWritten = 0;

        CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            synchronized (GlintUploadCountingRequestBody.class) {
                bytesWritten += byteCount;
                UiKit.runOnMainThreadAsync(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            try {
                                long time = System.currentTimeMillis();
                                if (time > lastRefreshTime) {
                                    long tempSpeed = (bytesWritten - lastBytesWritten) * 1000 / (time - lastRefreshTime);
                                    if (tempSpeed > 0) {
                                        speed = tempSpeed;
                                    }
                                }
                                lastRefreshTime = time;
                                lastBytesWritten = bytesWritten;
                                listener.onProgress(bytesWritten, contentLength(), speed, (int) (bytesWritten * 100 / contentLength()));
                            } catch (Exception e) {
                                e.printStackTrace();
                                listener.onFail(e);
                            }
                        }
                    }
                });
            }
        }
    }
}
