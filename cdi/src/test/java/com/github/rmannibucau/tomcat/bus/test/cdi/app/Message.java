package com.github.rmannibucau.tomcat.bus.test.cdi.app;

import java.io.Serializable;

public class Message implements Serializable {
    private final String value;

    public Message(final String s) {
        value = s;
    }

    @Override
    public String toString() {
        return "Message{" + value + '}';
    }
}
