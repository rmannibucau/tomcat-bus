package com.github.rmannibucau.tomcat.bus.cdi.internal;

import com.github.rmannibucau.tomcat.bus.cdi.Sender;
import org.apache.deltaspike.core.util.bean.BeanBuilder;
import org.apache.deltaspike.core.util.metadata.builder.ContextualLifecycle;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.LinkedList;

public class TomcatBusExtension implements Extension {
    private Collection<Bean<?>> beans = new LinkedList<Bean<?>>();

    <T> void process(final @Observes ProcessAnnotatedType<T> pat, final BeanManager beanManager) {
        final AnnotatedType<T> annotatedType = pat.getAnnotatedType();
        final Class<T> javaClass = annotatedType.getJavaClass();
        if (javaClass.isInterface() && (annotatedType.isAnnotationPresent(Sender.class) || javaClass.getAnnotation(Sender.class) != null)) {
            beans.add(
                new BeanBuilder<T>(beanManager)
                    .readFromType(annotatedType)
                    .passivationCapable(true)
                    .beanLifecycle(new SenderLifecycle<T>(javaClass))
                    .create());
        }
    }

    void addBeans(final @Observes AfterBeanDiscovery afterBeanDiscovery) {
        for (final Bean<?> bean : beans) {
            afterBeanDiscovery.addBean(bean);
        }
        beans.clear();
    }

    private static class SenderLifecycle<T> implements ContextualLifecycle<T> {
        private final Class<?>[] proxyApi;

        public SenderLifecycle(final Class<T> javaClass) {
            proxyApi = new Class<?>[] { javaClass };
        }

        @Override
        public T create(final Bean<T> bean, final CreationalContext<T> creationalContext) {
            return (T) Proxy.newProxyInstance(bean.getBeanClass().getClassLoader(), proxyApi, SenderHandler.INSTANCE);
        }

        @Override
        public void destroy(final Bean<T> bean, final T instance, final CreationalContext<T> creationalContext) {
            // no-op
        }
    }
}
