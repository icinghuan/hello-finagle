namespace java com.icinghuan.hello.finagle

service HelloService {

    string ping();

    string hello(1: string name);
}