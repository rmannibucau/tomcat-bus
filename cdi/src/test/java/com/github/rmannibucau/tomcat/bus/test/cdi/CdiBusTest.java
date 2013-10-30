package com.github.rmannibucau.tomcat.bus.test.cdi;

import com.github.rmannibucau.tomcat.bus.cdi.Sender;
import com.github.rmannibucau.tomcat.bus.cdi.internal.TomcatBusExtension;
import com.github.rmannibucau.tomcat.bus.test.cdi.app.AppSender;
import com.github.rmannibucau.tomcat.bus.test.cdi.app.Message;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.ziplock.IO;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class CdiBusTest {
    @Deployment(name = "war1")
    @TargetsContainer("tomee-1")
    public static Archive<?> createDep1() {
        return war();
    }

    @Deployment(name = "war2")
    @TargetsContainer("tomee-2")
    public static Archive<?> createDep2() {
        return war();
    }

    @Inject
    private AppSender sender;

    @Test
    @OperateOnDeployment("war1")
    public void checkMessageOnBothInstances() throws IOException {
        doTest("#1");
    }

    @Test
    @OperateOnDeployment("war2")
    public void testRunningInDep1() throws IOException {
        doTest("#2");
    }

    private static Archive<?> war() {
        return ShrinkWrap.create(WebArchive.class, "app.war")
                // test app
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addPackage(AppSender.class.getPackage())
                // extension
            .addPackages(true, Sender.class.getPackage())
            .addAsServiceProvider(Extension.class, TomcatBusExtension.class)
                // dependencies
            .addAsLibraries(
                jarLocation(BeanProvider.class),
                jarLocation(IO.class));
    }

    private void doTest(final String msg) throws IOException {
        sender.fire(new Message(msg));
        for (final int port : new int[] { 1234, 5678}) { // we get the url of a single instance so we forced ports in the config
            final URL url = new URL("http://localhost:" + port + "/app/report");
            final String report = IO.slurp(url);
            assertThat(report, containsString(msg));
        }
    }
}
