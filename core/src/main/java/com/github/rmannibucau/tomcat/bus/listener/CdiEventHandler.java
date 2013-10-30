package com.github.rmannibucau.tomcat.bus.listener;

import com.github.rmannibucau.tomcat.bus.api.EventHandler;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;

public class CdiEventHandler implements EventHandler {
    @Override
    public <T extends Serializable> void handle(final T message) {
        try {
            final BeanManager beanManager = BeanManager.class.cast(new InitialContext().lookup("java:comp/BeanManager"));
            beanManager.fireEvent(message);
        } catch (final NamingException e) {
            throw new IllegalStateException("Can't find bean manager");
        }
    }
}
