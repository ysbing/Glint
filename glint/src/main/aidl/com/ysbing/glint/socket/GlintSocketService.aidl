package com.ysbing.glint.socket;

import com.ysbing.glint.socket.GlintSocketBuilderWrapper;

interface GlintSocketService {

    void send(GlintSocketBuilderWrapper builderWrapper);

    void connect(String url);

    void on(GlintSocketBuilderWrapper builderWrapper);

    void off(String url, String cmdId, int tag);

    void offAll();
}
