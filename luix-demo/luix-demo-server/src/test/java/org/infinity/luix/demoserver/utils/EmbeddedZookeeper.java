//package org.infinity.luix.demoserver.utils;
//
//import org.apache.zookeeper.server.ServerConfig;
//import org.apache.zookeeper.server.ZooKeeperServerMain;
//import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Properties;
//
//public class EmbeddedZookeeper {
//    private ZooKeeperServerMain zookeeperServer;
//
//    public void start() throws IOException, QuorumPeerConfig.ConfigException {
//        Properties properties = new Properties();
//        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
//        properties.load(in);
//
//        QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
//        quorumConfiguration.parseProperties(properties);
//        in.close();
//
//        zookeeperServer = new ZooKeeperServerMain();
//        final ServerConfig configuration = new ServerConfig();
//        configuration.readFrom(quorumConfiguration);
//
//        Thread t = new Thread(() -> {
//            try {
//                zookeeperServer.runFromConfig(configuration);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//        t.start();
//    }
//}