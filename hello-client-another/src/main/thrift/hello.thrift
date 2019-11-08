namespace java com.icinghuan.hello.finagle

service HelloServiceAnother {

    string ping();

    string hello(1: string name);
}