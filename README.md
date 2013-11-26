# Goal

Be able to use Tomcat cluster to send/receive messages.

# Usage

`com.github.rmannibucau.tomcat.bus.listener.Sender` class has a static utility method `send`
which let you send any `Serializable` message you want.

To receive the message you depend on the `com.github.rmannibucau.tomcat.bus.api.EventHandler`
implementation you use. By default it will try to get a CDI `BeanManager` and will fire the sent message.
This way you can just `@Observes` messages. To override this behavior just implement `EventHandler`
and define your implementation qualified name in `META-INF/services/com.github.rmannibucau.tomcat.bus.api.EventHandler`.

# Installation

Just add tomcat-bus-core.jar in your tomcat `common.loader` and add in your server.xml the listener
`com.github.rmannibucau.tomcat.bus.listener.TomcatBusListener`:

```xml
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="com.github.rmannibucau.tomcat.bus.listener.TomcatBusListener" />

  <Service name="Catalina">
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
    <Engine name="Catalina" defaultHost="localhost">
      <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster"/>

      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">

      </Host>
    </Engine>
  </Service>
</Server>

```

The listener supports the following configurations:

* includePackages: comma separated list of event package which should be taken into account - (only relevant if you send children of `ClusterMessage`)
* excludePackages: comma separated list of event package which should be ignored (by default tomcat and tomee internal events are ignored) - (only relevant if you send children of `ClusterMessage`)
* eventHandler: the qualified name of the event handler you want to use (by default CDI one)

Note: using `META-INF/services/com.github.rmannibucau.tomcat.bus.api.EventHandler` SPI you can provide the handler in your application.

# CDI

`tomcat-bus-cdi.jar` provides an integration with CDI allowing you to send message from an interface ignoring the `Sender` API:

```java
@com.github.rmannibucau.tomcat.bus.cdi.Sender
public interface MySender {
    void claimSomething(Something s);
}
```

Then calling `mySender.claimSomething(s);` you'll send a message over the cluster. To get `MySender`
just use `@Inject`:

```java
public class Foo {
    @Inject
    private MySender sender;
}
```

Note: `tomcat-bus-cdi` doesn't need to be in the container but can be delivered with applications (it is recommanded since it depends on deltaspike).

# Sample project

```
.
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── org
    │   │       └── superbiz
    │   │           ├── MyMessage.java
    │   │           ├── Observer.java
    │   │           └── ServletEmitter.java
    │   ├── resources
    │   └── webapp
    │       └── WEB-INF
    │           └── beans.xml
    └── test
        └── java
```

* `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.superbiz</groupId>
  <artifactId>demo</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>war</packaging>

  <dependencies>
    <dependency>
      <groupId>org.apache.openejb</groupId>
      <artifactId>javaee-api</artifactId>
      <version>6.0-5</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>com.github.rmannibucau</groupId>
      <artifactId>tomcat-bus</artifactId>
      <version>${bus.version}</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-war-plugin</artifactId>
        <version>2.4</version>
        <configuration>
          <failOnMissingWebXml>false</failOnMissingWebXml>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
</project>
```

* `MyMessage`

```java
package org.superbiz;

import java.io.Serializable;

public class MyMessage implements Serializable {
    private final String msg;

    public MyMessage(final String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "MyMessage{" +
            "msg='" + msg + '\'' +
            '}';
    }
}

```

* `ServletEmitter` (the sender)

```java
package org.superbiz;

import com.github.rmannibucau.tomcat.bus.listener.Sender;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

@WebServlet(urlPatterns = "/send")
public class ServletEmitter extends HttpServlet {
    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        Sender.send(new MyMessage("Message @ " + new Date()));
    }
}
```

* `Observer`

```java
package org.superbiz;

import javax.enterprise.event.Observes;

public class Observer {
    public void listen(@Observes MyMessage message) {
        System.out.println(">>> " + message);
    }
}
```
