package com.ysbing.glint.socket.socketio;

public class Protocol {
    private final static int HEADER = 5;

    public static String encode(int id, String route, String msg) {
        if (route.length() > 255) {
            throw new RuntimeException("route max length is overflow.");
        }
        byte[] arr = new byte[HEADER + route.length()];
        int index = 0;
        arr[index++] = (byte) ((id >> 24) & 0xFF);
        arr[index++] = (byte) ((id >> 16) & 0xFF);
        arr[index++] = (byte) ((id >> 8) & 0xFF);
        arr[index++] = (byte) (id & 0xFF);
        arr[index++] = (byte) (route.length() & 0xFF);

        for (int i = 0; i < route.length(); i++) {
            arr[index++] = (byte) route.codePointAt(i);
        }
        return bt2Str(arr, arr.length) + msg;
    }

    private static String bt2Str(byte[] arr, int end) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < arr.length && i < end; i++) {
            buff.append(String.valueOf(Character.toChars((arr[i] + 256) % 256)));
        }
        return buff.toString();
    }

}