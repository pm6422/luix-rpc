package org.infinity.app.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

public class AppServerLauncher {
    public static void main(String[] args) throws IOException {
        // 启动嵌入式zk
        startZooKeeper();
        // 启动Spring容器
        new ClassPathXmlApplicationContext("classpath:application.xml");
    }

    private static void startZooKeeper() throws IOException {
        Properties properties = readProperty("rpc.properties");
        String property = properties.getProperty("registry.address");
        String[] hostAndPortParts = property.split(":");
        int zkPort = Integer.valueOf(hostAndPortParts[1]);
        if (hostAndPortParts[0].equalsIgnoreCase("localhost") || hostAndPortParts[0].equalsIgnoreCase("127.0.0.1")) {
            // Start embedded zookeeper server
            new EmbeddedZooKeeper(zkPort, false).start();
        }
    }

    private static Properties readProperty(String resourceName) throws IOException {
        return PropertiesLoaderUtils.loadAllProperties(resourceName);
    }
}
