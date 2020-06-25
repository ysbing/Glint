package com.ysbing.glint.socket;

public class SocketInnerResultBean {
    /**
     * 服务端响应内容
     */
    public String response;
    /**
     * 消息类型,0是普通消息，1是事件消息
     */
    public int msgType;
}