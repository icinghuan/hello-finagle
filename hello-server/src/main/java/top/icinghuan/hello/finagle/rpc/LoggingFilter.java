package top.icinghuan.hello.finagle.rpc;

import com.twitter.finagle.Service;
import com.twitter.finagle.SimpleFilter;
import com.twitter.util.Duration;
import com.twitter.util.Future;
import com.twitter.util.FutureEventListener;
import com.twitter.util.Stopwatch$;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.transport.TMemoryInputTransport;
import scala.Function0;

@Slf4j
public class LoggingFilter extends SimpleFilter<byte[], byte[]> {

    @Override
    public Future<byte[]> apply(final byte[] request, Service<byte[], byte[]> service) {
        final Function0<Duration> start = Stopwatch$.MODULE$.start();

        Future<byte[]> future = service.apply(request);
        future.addEventListener(new FutureEventListener<byte[]>() {

            @Override
            public void onSuccess(byte[] value) {
                Duration elapsed = start.apply();
                TMemoryInputTransport inputTransport = new TMemoryInputTransport(request);
                TBinaryProtocol iprot = new TBinaryProtocol(inputTransport);
                String name = null;
                try {
                    TMessage message = iprot.readMessageBegin();
                    name = message.name;
                } catch (TException e) {
                    log.warn("Failed to get method name.", e);
                }

                log.debug("Method {} elapsed {} millis.", name, elapsed.inMilliseconds());
            }

            @Override
            public void onFailure(Throwable cause) {
                TBinaryProtocol iprot = new TBinaryProtocol(new TMemoryInputTransport(request));
                String name = null;
                try {
                    TMessage message = iprot.readMessageBegin();
                    name = message.name;
                } catch (TException e) {
                    log.warn("Failed to get method name.", e);
                }

                log.error("Method {} failed, error message: {}.", name, cause.getMessage());
            }
        });

        return future;
    }
}
