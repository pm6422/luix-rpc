package org.infinity.rpc.spring.boot;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@EnableConfigurationProperties({InfinityProperties.class})
public class RpcAutoConfiguration {
    @Autowired
    private InfinityProperties infinityProperties;
}
