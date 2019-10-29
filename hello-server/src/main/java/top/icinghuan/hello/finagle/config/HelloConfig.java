package top.icinghuan.hello.finagle.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloConfig {

    public static final String SERVER_ZK_HOST = "hello.server.zkHost";
    public static final String SERVER_ZK_PATH = "hello.server.zkPath";
    public static final String SERVER_PORT = "hello.server.port";

    private static Config config;

    static {
        config = ConfigFactory.load();
    }

    public static int getListeningPort() {
        return config.getInt(SERVER_PORT);
    }

    public static String getZkHost() {
        return config.getString(SERVER_ZK_HOST);
    }

    public static String getZkPath() {
        return config.getString(SERVER_ZK_PATH);
    }
}
