package com.github.rmannibucau.tomcat.bus.message;

import java.io.Serializable;

public class Message implements Serializable {
    private String value;

    public Message(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
