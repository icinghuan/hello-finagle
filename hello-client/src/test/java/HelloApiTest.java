import com.icinghuan.hello.finagle.HelloService;
import com.twitter.finagle.Thrift;
import com.twitter.finagle.param.HighResTimer;
import com.twitter.finagle.service.RetryExceptionsFilter;
import com.twitter.finagle.service.RetryPolicy;
import com.twitter.finagle.stats.NullStatsReceiver;
import com.twitter.finagle.thrift.ThriftClientRequest;
import com.twitter.util.Duration;
import com.twitter.util.Future;
import com.twitter.util.FutureEventListener;
import com.twitter.util.Try;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.junit.Before;
import org.junit.Test;
import scala.runtime.Nothing$;

/**
 * @author : xuyuan
 * @date : 2019-10-29
 * Description :
 */
public class HelloApiTest {

    private HelloService.FutureIface helloService;

    @Before
    public void setup() {
        RetryPolicy<Try<Nothing$>> retryPolicy = RetryPolicy.tries(3, RetryPolicy.TimeoutAndWriteExceptionsOnly());
        RetryExceptionsFilter<ThriftClientRequest, byte[]> retryExceptionsFilter =
                new RetryExceptionsFilter<>(retryPolicy, HighResTimer.Default(), new NullStatsReceiver());

        helloService = Thrift
                .client()
                .withSessionQualifier().noFailFast()
                .withSessionQualifier().noFailureAccrual()
                .withProtocolFactory(new TBinaryProtocol.Factory())
                .withRequestTimeout(Duration.fromMilliseconds(10000))
                .withSession().acquisitionTimeout(Duration.fromMilliseconds(10000))
                .withSessionPool().minSize(3)
                .filtered(retryExceptionsFilter)
//                .newIface("localhost:9396", "hello-server", HelloService.FutureIface.class);
                .newIface("zk!localhost:2181!/service/hello", "hello-server", HelloService.FutureIface.class);
    }

    @Test
    public void testPing() throws InterruptedException {
        while (true) {
            helloService.ping();
            System.out.println("ping");
            Thread.sleep(10);
        }
    }

    @Test
    public void testHello() {
        System.out.println(helloService.hello("world").apply());
    }

    @Test
    public void testFlatMap() {
        Future<Integer> futureIntegerA = Future.value(1);
        Future<Integer> futureIntegerB = futureIntegerA.flatMap(this::multiplyTwo);
        System.out.println(futureIntegerA.get());
        System.out.println(futureIntegerB.get());
    }

    private Future<Integer> multiplyTwo(int i) {
        return Future.value(i >> 1);
    }

    @Test
    public void testCallback() {
        helloService.ping().addEventListener(new FutureEventListener<String>() {
            @Override
            public void onFailure(Throwable cause) {
                System.out.println(cause);
            }

            @Override
            public void onSuccess(String value) {
                System.out.println(value);
            }
        });
    }
}
