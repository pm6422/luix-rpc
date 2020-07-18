package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.config.spring.startup.NettyServerApplicationRunner;
import org.infinity.rpc.core.registry.Registrable;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@EnableConfigurationProperties({InfinityProperties.class})
public class RpcAutoConfiguration {
    @Autowired
    private InfinityProperties infinityProperties;

    /**
     * Create registry urls
     *
     * @param infinityProperties configuration properties
     * @return registry urls
     */
    @Bean
    public List<Url> registryUrls(InfinityProperties infinityProperties) {
        Url registryUrl = Url.of(infinityProperties.getRegistry().getName().getValue(),
                infinityProperties.getRegistry().getHost(),
                infinityProperties.getRegistry().getPort(),
                Registrable.class.getName());

        // Assign values to parameters
        registryUrl.addParameter(Url.PARAM_CHECK_HEALTH, Url.PARAM_CHECK_HEALTH_DEFAULT_VALUE);
        registryUrl.addParameter(Url.PARAM_ADDRESS, registryUrl.getAddress());
        registryUrl.addParameter(Url.PARAM_CONNECT_TIMEOUT, infinityProperties.getRegistry().getConnectTimeout().toString());
        registryUrl.addParameter(Url.PARAM_SESSION_TIMEOUT, infinityProperties.getRegistry().getSessionTimeout().toString());
        registryUrl.addParameter(Url.PARAM_RETRY_INTERVAL, infinityProperties.getRegistry().getRetryInterval().toString());
        // TODO: Support multiple registry centers
        return Arrays.asList(registryUrl);
    }

    @Bean
    public RpcConsumerProxy rpcConsumerProxy(InfinityProperties infinityProperties) {
        List<Url> registryUrls = registryUrls(infinityProperties);
        List<Registry> registries = new ArrayList<>();
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactoryImpl = RegistryFactory.getInstance(infinityProperties.getRegistry().getName().getValue());
            registries.add(registryFactoryImpl.getRegistry(registryUrl));
        }
        return new RpcConsumerProxy(registries);
    }

    @Bean
    public ApplicationRunner nettyServerApplicationRunner() {
        return new NettyServerApplicationRunner();
    }
}
