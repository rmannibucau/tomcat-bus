package com.github.rmannibucau.tomcat.bus.listener;

import org.apache.catalina.ha.ClusterMessageBase;

public class MessageWrapper extends ClusterMessageBase {
    private byte[] bytes;
    private String context;

    public byte[] getMessage() {
        return bytes;
    }

    public void setMessage(final byte[] message) {
        this.bytes = message;
    }

    public String getContext() {
        return context;
    }

    public void setContext(final String context) {
        this.context = context;
    }
}
