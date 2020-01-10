package org.infinity.rpc.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.registrar.RpcConsumerFactoryBean;
import org.infinity.rpc.registry.ZkRegistryRpcServerDiscovery;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.Map;

//import org.infinity.rpc.client.annotation.Consumer;

@Slf4j
@EnableConfigurationProperties({RpcClientProperties.class})
@Configuration
public class RpcClientConfiguration implements ApplicationContextAware {

    @Autowired
    private RpcClientProperties rpcClientProperties;
    private ApplicationContext  applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public ZkRegistryRpcServerDiscovery rpcServerDiscovery() throws Exception {
        ZkRegistryRpcServerDiscovery zkRegistryRpcServerDiscovery = new ZkRegistryRpcServerDiscovery(rpcClientProperties.getRegistry().getAddress());
        this.setRpcServerDiscovery(zkRegistryRpcServerDiscovery);
        return zkRegistryRpcServerDiscovery;
    }

    private void setRpcServerDiscovery(ZkRegistryRpcServerDiscovery rpcServerDiscovery) {
        Map<String, RpcConsumerFactoryBean> beansOfType = applicationContext.getBeansOfType(RpcConsumerFactoryBean.class);
        if (CollectionUtils.isEmpty(beansOfType)) {
            return;
        }
        beansOfType.values().forEach(val -> val.setZkRegistryRpcServerDiscovery(rpcServerDiscovery));
    }

}
