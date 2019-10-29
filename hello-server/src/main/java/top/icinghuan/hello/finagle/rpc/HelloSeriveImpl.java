package top.icinghuan.hello.finagle.rpc;

import com.icinghuan.hello.finagle.HelloService;
import com.twitter.util.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author : xuyuan
 * @date : 2019-08-25
 * Description :
 */
@Component
@Slf4j
public class HelloSeriveImpl implements HelloService.FutureIface {

    @Override
    public Future<String> ping() {
        return Future.value("pong");
    }

    @Override
    public Future<String> hello(String name) {
        return Future.value("hello " + name);
    }
}
