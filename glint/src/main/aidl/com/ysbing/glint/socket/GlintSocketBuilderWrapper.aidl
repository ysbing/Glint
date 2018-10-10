package com.ysbing.glint.socket;

interface GlintSocketBuilderWrapper {

    String getUrl();

    String getCmdId();

    String getParams();

    int getSendId();

    int getTag();

    void onResponse(String response);

    void onError(String error);

}
