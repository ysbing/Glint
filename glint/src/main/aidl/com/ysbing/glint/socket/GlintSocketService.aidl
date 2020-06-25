package com.ysbing.glint.socket;

import com.ysbing.glint.socket.GlintSocketBuilderWrapper;

interface GlintSocketService {

    void connect(String url);

    void connectIO(String url);

    void send(GlintSocketBuilderWrapper builderWrapper);

    void sendIO(GlintSocketBuilderWrapper builderWrapper);

    void on(GlintSocketBuilderWrapper builderWrapper);

    void onIO(GlintSocketBuilderWrapper builderWrapper);

    void off(String url, String cmdId, int tag);

    void offAll();
}
