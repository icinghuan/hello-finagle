package finagle.another.rpc;

import com.icinghuan.hello.finagle.HelloServiceAnother;
import com.twitter.util.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author : xuyuan
 * @date : 2019-08-25
 * Description :
 */
@Component
@Slf4j
public class HelloServiceImpl implements HelloServiceAnother.FutureIface {

    @Override
    public Future<String> ping() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Future.value("pong");
    }

    @Override
    public Future<String> hello(String name) {
        return Future.value("hello " + name);
    }
}
