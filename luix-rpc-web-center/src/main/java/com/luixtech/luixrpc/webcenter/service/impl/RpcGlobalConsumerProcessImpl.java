package com.luixtech.luixrpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.luixtech.luixrpc.core.listener.GlobalConsumerDiscoveryListener;
import com.luixtech.luixrpc.core.server.buildin.BuildInService;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.webcenter.domain.RpcConsumer;
import com.luixtech.luixrpc.webcenter.repository.RpcConsumerRepository;
import com.luixtech.luixrpc.webcenter.service.RpcApplicationService;
import com.luixtech.luixrpc.webcenter.service.RpcServerService;
import com.luixtech.luixrpc.webcenter.service.RpcServiceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcGlobalConsumerProcessImpl implements GlobalConsumerDiscoveryListener {

    @Resource
    private RpcConsumerRepository rpcConsumerRepository;
    @Resource
    private RpcServerService      rpcServerService;
    @Resource
    private RpcServiceService     rpcServiceService;
    @Resource
    private RpcApplicationService rpcApplicationService;

    @Override
    public void onNotify(Url registryUrl, String interfaceName, List<Url> consumerUrls) {
        if (CollectionUtils.isNotEmpty(consumerUrls)) {
            for (Url consumerUrl : consumerUrls) {
                RpcConsumer rpcConsumer = RpcConsumer.of(consumerUrl, registryUrl);
                if (BuildInService.class.getName().equals(rpcConsumer.getInterfaceName())) {
                    continue;
                }
                log.info("Discovered active consumers: {}", consumerUrl);
                // Insert or update consumer
                rpcConsumerRepository.save(rpcConsumer);

                // Insert server
                rpcServerService.insert(registryUrl, consumerUrl, rpcConsumer.getAddress());

                // Insert service
                rpcServiceService.insert(registryUrl, rpcConsumer.getInterfaceName());

                // Insert application
                rpcApplicationService.insert(registryUrl, consumerUrl, rpcConsumer.getApplication());
            }
        } else {
            // Update consumers to inactive
            List<RpcConsumer> list = rpcConsumerRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            log.info("Discovered inactive consumers of [{}]", interfaceName);

            list.forEach(provider -> provider.setActive(false));
            rpcConsumerRepository.saveAll(list);
        }
    }
}
