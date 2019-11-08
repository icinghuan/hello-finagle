package top.icinghuan.hello.finagle.config;

import com.icinghuan.hello.finagle.HelloServiceAnother;
import com.twitter.finagle.Thrift;
import com.twitter.util.Duration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HelloRpcConfiguration {

    @Bean
    @ConditionalOnMissingBean(HelloServiceAnother.FutureIface.class)
    public HelloServiceAnother.FutureIface helloRpc() {
        Config config = ConfigFactory.load();
        String serverAddr = config.getString("hello.serverAddr");
        HelloServiceAnother.FutureIface client = Thrift
                .client()
                .withProtocolFactory(new TBinaryProtocol.Factory())
                .withSession().acquisitionTimeout(Duration.fromMilliseconds(3000))
                .withSessionPool().minSize(1)
                .newIface(serverAddr, "hello-client", HelloServiceAnother.FutureIface.class);
        return client;
    }
}
