package com.github.rmannibucau.tomcat.bus.handler;

import com.github.rmannibucau.tomcat.bus.api.EventHandler;
import com.github.rmannibucau.tomcat.bus.message.Message;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SimpleEventHandler implements EventHandler {
    private final CountDownLatch latch = new CountDownLatch(1);

    private String message = null;

    @Override
    public <T extends Serializable> void handle(final T message) {
        latch.countDown();
        this.message = Message.class.cast(message).getValue();
    }

    public String getMessage() {
        return message;
    }

    public void await() {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            // no-op
        }
    }
}
