package com.ysbing.glint.socket.socketio;


import androidx.annotation.NonNull;

public class IOMessage {
    public static final int TYPE_DISCONNECT = 0;
    public static final int TYPE_CONNECT = 1;
    public static final int TYPE_HEARTBEAT = 2;
    public static final int TYPE_MESSAGE = 3;
    public static final int TYPE_JSON_MESSAGE = 4;
    public static final int TYPE_EVENT = 5;
    public static final int TYPE_ACK = 6;
    public static final int TYPE_ERROR = 7;
    public static final int TYPE_NOOP = 8;
    public static final int FIELD_TYPE = 0;
    public static final int FIELD_ID = 1;
    public static final int FIELD_ENDPOINT = 2;
    public static final int FIELD_DATA = 3;
    public static final int NUM_FIELDS = 4;
    private final String[] mFields;
    private int type;

    private IOMessage(int type, String id, String namespace, String data) {
        this.mFields = new String[4];
        this.type = type;
        this.mFields[1] = id;
        this.mFields[0] = "" + type;
        this.mFields[2] = namespace;
        this.mFields[3] = data;
    }

    public IOMessage(String message) {
        this.mFields = new String[4];
        String[] fields = message.split(":", 4);

        for (int i = 0; i < fields.length; ++i) {
            this.mFields[i] = fields[i];
            if (i == 0) {
                this.type = Integer.parseInt(fields[i]);
            }
        }

    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String field : this.mFields) {
            builder.append(':');
            if (field != null) {
                builder.append(field);
            }
        }
        return builder.substring(1);
    }

    public int getType() {
        return this.type;
    }

    public String getId() {
        return this.mFields[1];
    }

    public void setId(String id) {
        this.mFields[1] = id;
    }

    public String getEndpoint() {
        return this.mFields[2];
    }

    public String getData() {
        return this.mFields[3];
    }
}
