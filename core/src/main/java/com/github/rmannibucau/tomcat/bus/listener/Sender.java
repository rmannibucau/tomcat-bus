package com.github.rmannibucau.tomcat.bus.listener;

import java.io.Serializable;

public final class Sender {
    private static TomcatBusListener handler;

    static void setHandler(final TomcatBusListener handler) {
        Sender.handler = handler;
    }

    public static <T extends Serializable> void send(final T msg) {
        if (handler == null) {
            throw new IllegalStateException("Either you forgot to setup Tomcat clustering or you didn't set up " +
                "com.github.rmannibucau.tomcat.bus.listener.TomcatBusListener," +
                "add to your server.xml:" +
                "<Listener className=\"com.github.rmannibucau.tomcat.bus.listener.TomcatBusListener\" />");
        }
        handler.sendThroughTheCluster(msg);
    }
}
