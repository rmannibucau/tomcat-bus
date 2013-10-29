package com.github.rmannibucau.tomcat.bus.listener;

import java.io.Serializable;

public final class Sender {
    private static TomcatBusListener handler;

    static void setHandler(final TomcatBusListener handler) {
        Sender.handler = handler;
    }

    public static <T extends Serializable> void send(final T msg) {
        handler.sendThroughTheCluster(msg);
    }
}
