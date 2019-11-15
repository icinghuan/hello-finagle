package top.icinghuan.hello.finagle;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.icinghuan.hello.finagle.HelloService;
import com.tigerbrokers.alpha.commons.config.EnableConfig;
import com.twitter.finagle.ListeningServer;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableMBeanExport;
import top.icinghuan.hello.finagle.rpc.HelloServiceImpl;

import java.util.Properties;

/**
 * @author : xuyuan
 * @date : 2019-10-29
 * Description :
 */
@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableMBeanExport
@EnableConfig
@ComponentScan(basePackages = "top.icinghuan.hello")
public class HelloServer extends AbstractIdleService {

    private ListeningServer server;

    private HelloService.FutureIface futureIface = new HelloServiceImpl();

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
