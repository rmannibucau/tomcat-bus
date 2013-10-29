package com.github.rmannibucau.tomcat.bus.listener;

import com.github.rmannibucau.tomcat.bus.api.EventHandler;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.ha.CatalinaCluster;
import org.apache.catalina.ha.ClusterListener;
import org.apache.catalina.ha.ClusterMessage;
import org.apache.catalina.util.CustomObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.ServiceLoader;

public class TomcatBusListener implements LifecycleListener {
    private static final String[] EMPTY_ARRAY = new String[0];
    private static final String[] DEFAULT_EXCLUDE = new String[]{ "org.apache.tomcat.", "org.apache.tomee." };

    private String[] includePackages = EMPTY_ARRAY;
    private String[] excludePackages = DEFAULT_EXCLUDE;

    private CatalinaCluster catalinaCluster;
    private EventHandler eventHandler;
    private Server server;

    @Override
    public void lifecycleEvent(final LifecycleEvent event) {
        if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType()) && Server.class.isInstance(event.getSource())) {
            server = Server.class.cast(event.getSource());
            final Service[] services = server.findServices();
            for (final Service service : services) {
                final Container container = service.getContainer();
                final Cluster cluster = container.getCluster();
                if (cluster != null && CatalinaCluster.class.isInstance(cluster)) {
                    catalinaCluster = CatalinaCluster.class.cast(cluster);

                    if (eventHandler == null) {
                        final Iterator<EventHandler> iterator = ServiceLoader.load(EventHandler.class, TomcatBusListener.class.getClassLoader()).iterator();
                        if (iterator.hasNext()) {
                            eventHandler = iterator.next();
                        } else {
                            eventHandler = new CdiEventHandler();
                        }
                    }

                    Sender.setHandler(this);

                    catalinaCluster.addClusterListener(new ClusterListener() {
                        @Override
                        public void messageReceived(final ClusterMessage clusterMessage) {
                            propagateLocally(clusterMessage);
                        }

                        @Override
                        public boolean accept(final ClusterMessage clusterMessage) {
                            final String pkg = clusterMessage.getClass().getPackage().getName();
                            for (final String exclude : excludePackages) {
                                if (pkg.startsWith(exclude)) {
                                    return false;
                                }
                            }

                            if (includePackages.length == 0) {
                                return true;
                            }
                            for (final String include : includePackages) {
                                if (pkg.startsWith(include)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                }
            }
        }
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public <T extends Serializable> void propagateLocally(final T message) {
        if (MessageWrapper.class.isInstance(message)) {
            final MessageWrapper messageWrapper = MessageWrapper.class.cast(message);

            final ClassLoader loader = findContextLoader(messageWrapper.getContext());
            final byte[] bytes = messageWrapper.getMessage();

            final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(loader);
            try {
                eventHandler.handle(deserialize(loader, bytes));
            } catch (final Exception e) {
                throw new IllegalArgumentException(e);
            } finally {
                Thread.currentThread().setContextClassLoader(oldLoader);
            }
        } else {
            eventHandler.handle(message);
        }
    }

    public <T extends Serializable> void sendThroughTheCluster(final T message) {
        if (catalinaCluster == null) {
            throw new IllegalStateException("No cluster found");
        }

        if (ClusterMessage.class.isInstance(message)) {
            catalinaCluster.send(ClusterMessage.class.cast(message));
        } else {
            final Context context = findContext();

            final MessageWrapper wrapper = new MessageWrapper();
            if (context != null) {
                wrapper.setContext(context.getName());
            }
            try {
                wrapper.setMessage(serialize(message));
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }

            catalinaCluster.send(wrapper);
        }
        propagateLocally(message); // don't forget local node
    }

    private static Serializable deserialize(final ClassLoader loader, final byte[] bytes) throws IOException, ClassNotFoundException {
        final CustomObjectInputStream cois = new CustomObjectInputStream(new ByteArrayInputStream(bytes), loader);
        try {
            return Serializable.class.cast(cois.readObject());
        } finally {
            cois.close();
        }
    }

    private static <T extends Serializable> byte[] serialize(final T message) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        try {
            oos.writeObject(message);
        } finally {
            oos.close();
        }
        return baos.toByteArray();
    }

    private ClassLoader findContextLoader(final String name) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (final Service service : server.findServices()) {
            final Container container = service.getContainer();
            if (Engine.class.isInstance(container)) {
                for (final Container host : container.findChildren()) {
                    if (Host.class.isInstance(host)) {
                        for (final Container context : host.findChildren()) {
                            if (Context.class.isInstance(context)) {
                                final Context castedContext = Context.class.cast(context);
                                if (castedContext.getName().equals(name)) {
                                    return castedContext.getLoader().getClassLoader();
                                }
                            }
                        }
                    }
                }
            }
        }
        return loader;
    }

    private Context findContext() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (final Service service : server.findServices()) {
            final Container container = service.getContainer();
            if (Engine.class.isInstance(container)) {
                for (final Container host : container.findChildren()) {
                    if (Host.class.isInstance(host)) {
                        for (final Container context : host.findChildren()) {
                            if (Context.class.isInstance(context)) {
                                final Context castedContext = Context.class.cast(context);
                                if (castedContext.getLoader().getClassLoader() == loader) {
                                    return castedContext;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void setIncludePackages(final String value) {
        if (value == null) {
            includePackages = EMPTY_ARRAY;
        } else {
            includePackages = toArray(value);
        }
    }

    public void setExcludePackages(final String value) {
        if (value == null) {
            excludePackages = DEFAULT_EXCLUDE;
        } else {
            excludePackages = toArray(value);
        }
    }

    public void setEventHandler(final String classname) {
        try {
            final Class<?> clazz = TomcatBusListener.class.getClassLoader().loadClass(classname);
            eventHandler = EventHandler.class.cast(clazz.newInstance());
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String[] toArray(final String value) {
        final String[] split = value.split(",");
        final String[] includePackages = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            includePackages[i] = split[i].trim();
        }
        return includePackages;
    }
}
