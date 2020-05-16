package org.infinity.rpc.core.config.spring.startup;

import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.netty.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

public class NettyServerApplicationRunner implements ApplicationRunner, Ordered {

    @Autowired
    private InfinityProperties infinityProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        NettyServer nettyServer = new NettyServer(infinityProperties.getProtocol().getHost(), infinityProperties.getProtocol().getPort());
        nettyServer.startNettyServer();
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}