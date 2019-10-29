package top.icinghuan.hello.finagle;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.icinghuan.hello.finagle.HelloService;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.finagle.Thrift;
import com.twitter.finagle.thrift.Protocols;
import com.twitter.util.Duration;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import top.icinghuan.hello.finagle.config.HelloConfig;
import top.icinghuan.hello.finagle.rpc.LoggingFilter;

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
public class HelloServer extends AbstractIdleService {

    private ListeningServer server;

    @Autowired
    private HelloService.FutureIface futureIface;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HelloServer.class);

        Properties properties = new Properties();
        properties.put("spring.profiles.active", ConfigFactory.load().getString("env"));

        application.setDefaultProperties(properties);

        ConfigurableApplicationContext context = application.run(args);

        context.registerShutdownHook();

        HelloServer helloServer = context.getBean(HelloServer.class);
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
        HelloService.FinagledService finagledService = new HelloService.FinagledService(futureIface,
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
