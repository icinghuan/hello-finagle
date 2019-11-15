package top.icinghuan.hello.finagle.config;

import com.icinghuan.hello.finagle.HelloService;
import com.tigerbrokers.alpha.commons.config.ConfigBean;
import com.tigerbrokers.alpha.commons.config.ConfigValue;
import com.tigerbrokers.alpha.finagle.server.AdmissionControlParams;
import com.tigerbrokers.alpha.finagle.server.FinagleServerBuilder;
import com.tigerbrokers.alpha.finagle.server.FinagleServerConfig;
import com.tigerbrokers.alpha.finagle.server.LoggingParams;
import com.tigerbrokers.alpha.metrics.finagle.MetricsFilter;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Thrift;
import com.twitter.finagle.thrift.Protocols;
import com.twitter.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * @author : xuyuan
 * @date : 2019-11-15
 * Description :
 */
@Slf4j
@Setter
@Configuration
public class RpcServerConfig {

    @ConfigValue("hello.server.zkHost")
    private String zkHost = "localhost:2181";

    @ConfigValue("hello.server.zkPath")
    private String zkPath = "/service/hello";

    @ConfigBean("hello.server.config")
    private FinagleServerConfig serverConfig = new FinagleServerConfig();

    @Autowired(required = false)
    private MetricsFilter metricsFilter;

    public RpcServerConfig() {
        serverConfig.setPort(9396);
        serverConfig.setRequestTimeoutMillis(1000L);

        AdmissionControlParams admissionControlParams = new AdmissionControlParams();
        admissionControlParams.setMaxConcurrentRequests(1);
        admissionControlParams.setMaxWaiters(100);
        admissionControlParams.setDeadlineToleranceSeconds(5);
        admissionControlParams.setDeadlineMaxRejectedPercentage(0.2);
        serverConfig.setAdmissionControlParams(admissionControlParams);

        LoggingParams loggingParams = new LoggingParams();
        loggingParams.setLoggingMinDurationMillis(500);
        loggingParams.setLoggingMaxSizeByte(10000);
        serverConfig.setLoggingParams(loggingParams);
    }

    @Autowired
    private HelloService.FutureIface helloService;

    @Bean
    public ListeningServer rpcServer() {
        HelloService.FinagledService service =
                new HelloService.FinagledService(
                        helloService,
                        Protocols.binaryFactory(
                                Protocols.binaryFactory$default$1(),
                                Protocols.binaryFactory$default$2(),
                                Protocols.binaryFactory$default$3(),
                                Protocols.binaryFactory$default$4())
                );

        ListeningServer server = FinagleServerBuilder.build("hello-server", service, metricsFilter, serverConfig);
        server.announce(String.format("zk!%s!%s!0", zkHost, zkPath)).get();
        return server;
    }
}
