package com.github.rmannibucau.tomcat.bus.api;

import java.io.Serializable;

public interface EventHandler {
    <T extends Serializable> void handle(final T message);
}
