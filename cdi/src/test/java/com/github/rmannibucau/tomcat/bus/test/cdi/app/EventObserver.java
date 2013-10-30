package com.github.rmannibucau.tomcat.bus.test.cdi.app;

import javax.enterprise.event.Observes;

public class EventObserver {
    void catchMyMessage(final @Observes Message message) {
        ReportServlet.newMessage(message);
    }
}
