package com.github.rmannibucau.tomcat.bus.test.cdi.app;

import com.github.rmannibucau.tomcat.bus.cdi.Sender;

@Sender
public interface AppSender {
    void fire(Message msg);
}
