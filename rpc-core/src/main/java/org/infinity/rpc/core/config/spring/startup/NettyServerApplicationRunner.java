package org.infinity.rpc.core.config.spring.startup;

import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.netty.NettyServer;
import org.infinity.rpc.core.config.spring.server.ProviderWrapperHolder;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

//public class NettyServerApplicationRunner implements ApplicationRunner, Ordered {
//
//    @Autowired
//    private InfinityProperties infinityProperties;
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        // Delayed exposure providers
//        SwitcherService.getInstance().setValue(SwitcherService.REGISTRY_HEARTBEAT_SWITCHER, true);
//
//        if (MapUtils.isNotEmpty(ProviderWrapperHolder.getInstance().getWrappers())) {
//            NettyServer nettyServer = new NettyServer(infinityProperties.getProtocol().getHost(), infinityProperties.getProtocol().getPort());
//            nettyServer.startNettyServer();
//        }
//    }
//
//    @Override
//    public int getOrder() {
//        // Higher values are interpreted as lower priority
//        return Integer.MAX_VALUE;
//    }
//}