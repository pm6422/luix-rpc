package org.infinity.rpc.core.config.spring;

import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.netty.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

public class NettyServerApplicationRunner implements ApplicationRunner {

    @Autowired
    private InfinityProperties infinityProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        NettyServer nettyServer = new NettyServer(infinityProperties.getProtocol().getHost(), infinityProperties.getProtocol().getPort());
        nettyServer.startNettyServer();
    }
}