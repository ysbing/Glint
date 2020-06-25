package com.ysbing.glint.socket;

interface GlintSocketBuilderWrapper {

    String getUrl();

    String getCmdId();

    String getParams();

    int getSendId();

    int getTag();

    String getResponseCmdId(String response);

    void onResponse(String response);

    void onError(String error);

}
