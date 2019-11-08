package finagle.another;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.icinghuan.hello.finagle.HelloServiceAnother;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.finagle.Thrift;
import com.twitter.finagle.thrift.Protocols;
import com.twitter.util.Duration;
import com.typesafe.config.ConfigFactory;
import finagle.another.config.HelloConfig;
import finagle.another.rpc.LoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * @author : xuyuan
 * @date : 2019-10-29
 * Description :
 */
@Slf4j
@Component
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class HelloServerAnother extends AbstractIdleService {

    private ListeningServer server;

    @Autowired
    private HelloServiceAnother.FutureIface futureIface;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HelloServerAnother.class);

        Properties properties = new Properties();
        properties.put("spring.profiles.active", ConfigFactory.load().getString("env"));

        application.setDefaultProperties(properties);

        ConfigurableApplicationContext context = application.run(args);

        context.registerShutdownHook();

        HelloServerAnother helloServer = context.getBean(HelloServerAnother.class);
        helloServer.startAsync();
    }

    @Override
    protected void startUp() {
        log.info("HelloServer starting...");
        try {
            start();
        } catch (Throwable ex) {
            log.error("HelloServer start failed", ex);
            throw ex;
        }
        log.info("HelloServer started");
    }

    private void start() {
        Preconditions.checkArgument(futureIface != null);

        int port = HelloConfig.getListeningPort();
        String zkHost = HelloConfig.getZkHost();
        String zkPath = HelloConfig.getZkPath();

        LoggingFilter loggingFilter = new LoggingFilter();
        HelloServiceAnother.FinagledService finagledService = new HelloServiceAnother.FinagledService(futureIface,
                Protocols.binaryFactory(
                        Protocols.binaryFactory$default$1(),
                        Protocols.binaryFactory$default$2(),
                        Protocols.binaryFactory$default$3(),
                        Protocols.binaryFactory$default$4()
                )
        );
        Service<byte[], byte[]> service = loggingFilter.andThen(finagledService);

        server = Thrift.server()
                .withLabel("hello-server")
                .withRequestTimeout(Duration.fromSeconds(30))
                .serve(new InetSocketAddress(port), service);

        server.announce(String.format("zk!%s!%s!0", zkHost, zkPath)).get();
    }

    @Override
    protected void shutDown() throws InterruptedException {
        log.info("HelloServer stopping...");
        try {
            stop();
            Thread.sleep(5000L);
        } catch (Throwable ex) {
            log.error("HelloServer stop error", ex);
            throw ex;
        }
        log.info("HelloServer stopped");
    }

    private void stop() {
        server.close();
    }
}
