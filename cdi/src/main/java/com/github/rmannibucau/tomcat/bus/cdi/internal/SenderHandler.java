package com.github.rmannibucau.tomcat.bus.cdi.internal;

import com.github.rmannibucau.tomcat.bus.listener.Sender;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class SenderHandler implements InvocationHandler {
    public static final InvocationHandler INSTANCE = new SenderHandler();

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (args.length != 1) {
            throw new IllegalArgumentException("Sender method can only get a single argument");
        }
        if (args[0] == null) {
            throw new IllegalArgumentException("Sender method can't send null");
        }
        if (!Serializable.class.isInstance(args[0])) {
            throw new IllegalArgumentException("Sender method can only send Serializable arguments");
        }

        Sender.send(Serializable.class.cast(args[0]));
        return null;
    }
}
