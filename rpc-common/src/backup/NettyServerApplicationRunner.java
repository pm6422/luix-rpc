package org.infinity.rpc.common;//package org.infinity.rpc.core.config.spring.startup;
//
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