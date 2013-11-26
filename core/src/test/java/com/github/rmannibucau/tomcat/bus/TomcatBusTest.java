package com.github.rmannibucau.tomcat.bus;

import com.github.rmannibucau.tomcat.bus.handler.SimpleEventHandler;
import com.github.rmannibucau.tomcat.bus.listener.Sender;
import com.github.rmannibucau.tomcat.bus.listener.TomcatBusListener;
import com.github.rmannibucau.tomcat.bus.message.Message;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11Protocol;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class TomcatBusTest {
    @Test
    public void test() throws Exception {
        final String message = "test #0";

        final Tomcat tomcat1 = newTomcat("tomcat1", 1234);
        final Tomcat tomcat2 = newTomcat("tomcat2", 5678);

        try {
            tomcat1.start();
            tomcat2.start();

            final TomcatBusListener listener1 = TomcatBusListener.class.cast(tomcat1.getServer().findLifecycleListeners()[0]);
            final TomcatBusListener listener2 = TomcatBusListener.class.cast(tomcat2.getServer().findLifecycleListeners()[0]);

            final SimpleEventHandler handler1 = SimpleEventHandler.class.cast(listener1.getDefaultEventHandler());
            final SimpleEventHandler handler2 = SimpleEventHandler.class.cast(listener2.getDefaultEventHandler());

            Sender.send(new Message(message)); // we'll use the last started listener to send the message but that's fine, we just need one

            handler1.await();
            handler2.await();

            assertEquals(message, handler1.getMessage());
            assertEquals(message, handler2.getMessage());
        } finally {
            destroy(tomcat1);
            destroy(tomcat2);
        }
    }

    private static void destroy(final Tomcat instance) throws LifecycleException {
        instance.stop();
        instance.destroy();
    }

    private static Tomcat newTomcat(final String base, final int port) throws LifecycleException {
        final Connector connector = new Connector(Http11Protocol.class.getName());
        connector.setPort(port);

        final TomcatBusListener listener = new TomcatBusListener();
        listener.setEventHandler(SimpleEventHandler.class.getName());

        final Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(mkdirs("target/" + base + "/conf"));
        tomcat.getHost().setAppBase(mkdirs("target/" + base + "/webapps"));
        tomcat.getEngine().setName(base);
        tomcat.getEngine().setCluster(new SimpleTcpCluster());
        tomcat.getServer().addLifecycleListener(listener);
        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);

        return tomcat;
    }

    private static String mkdirs(String base) throws LifecycleException {
        final File file = new File(base);
        if (!file.exists() && !file.mkdirs()) {
            throw new LifecycleException("Can't create workdir");
        }
        return file.getAbsolutePath();
    }

}
