package top.icinghuan.hello.finagle.rpc;

import com.icinghuan.hello.finagle.HelloService;
import com.icinghuan.hello.finagle.HelloServiceAnother;
import com.twitter.finagle.Thrift;
import com.twitter.util.Duration;
import com.twitter.util.Future;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : xuyuan
 * @date : 2019-08-25
 * Description :
 */
@Component
@Slf4j
public class HelloServiceImpl implements HelloService.FutureIface {

    private HelloServiceAnother.FutureIface helloRpc;

    public HelloServiceImpl() {
        Config config = ConfigFactory.load();
        String serverAddr = config.getString("hello.serverAddr");
        helloRpc = Thrift
                .client()
                .withProtocolFactory(new TBinaryProtocol.Factory())
                .withSession().acquisitionTimeout(Duration.fromMilliseconds(3000))
                .withSessionPool().minSize(1)
                .newIface(serverAddr, "hello-client", HelloServiceAnother.FutureIface.class);
    }

    private AtomicInteger reqNum = new AtomicInteger(0);

    @Override
    public Future<String> ping() {
        log.info("{}", reqNum.incrementAndGet());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        log.info(helloRpc.ping().apply(Duration.apply(1, TimeUnit.SECONDS)));
        return Future.value("pong");
    }

    @Override
    public Future<String> hello(String name) {
        return Future.value("hello " + name);
    }
}
