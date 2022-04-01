package org.infinity.luix.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.core.listener.GlobalConsumerDiscoveryListener;
import org.infinity.luix.core.server.buildin.BuildInService;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.webcenter.domain.RpcConsumer;
import org.infinity.luix.webcenter.repository.RpcConsumerRepository;
import org.infinity.luix.webcenter.repository.RpcServerRepository;
import org.infinity.luix.webcenter.repository.RpcServiceRepository;
import org.infinity.luix.webcenter.service.RpcApplicationService;
import org.infinity.luix.webcenter.service.RpcServerService;
import org.infinity.luix.webcenter.service.RpcServiceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcGlobalConsumerProcessImpl implements GlobalConsumerDiscoveryListener {

    @Resource
    private RpcConsumerRepository rpcConsumerRepository;
    @Resource
    private RpcServerRepository   rpcServerRepository;
    @Resource
    private RpcServiceRepository  rpcServiceRepository;
    @Resource
    private RpcServerService      rpcServerService;
    @Resource
    private RpcServiceService     rpcServiceService;
    @Resource
    private RpcApplicationService rpcApplicationService;

    @Override
    public void onNotify(Url registryUrl, String interfaceName, List<Url> consumerUrls) {
        if (CollectionUtils.isNotEmpty(consumerUrls)) {
            log.info("Discovered active consumers {}", consumerUrls);
            for (Url consumerUrl : consumerUrls) {
                RpcConsumer rpcConsumer = RpcConsumer.of(consumerUrl, registryUrl);
                if (BuildInService.class.getName().equals(rpcConsumer.getInterfaceName())) {
                    continue;
                }
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
            log.info("Discovered offline consumers of [{}]", interfaceName);

            // Update consumers to inactive
            List<RpcConsumer> list = rpcConsumerRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            list.forEach(provider -> provider.setActive(false));
            rpcConsumerRepository.saveAll(list);

            // Update service to inactive
            rpcServiceService.inactivate(list.get(0).getRegistryIdentity(), list.get(0).getInterfaceName());

            // Update application to inactive
            rpcApplicationService.inactivate(list.get(0).getRegistryIdentity(), list.get(0).getApplication());
        }
    }
}
