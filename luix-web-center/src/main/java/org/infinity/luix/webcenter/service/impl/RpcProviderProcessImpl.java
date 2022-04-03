package org.infinity.luix.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.core.listener.GlobalProviderDiscoveryListener;
import org.infinity.luix.core.server.buildin.BuildInService;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.webcenter.domain.RpcProvider;
import org.infinity.luix.webcenter.repository.RpcProviderRepository;
import org.infinity.luix.webcenter.service.RpcApplicationService;
import org.infinity.luix.webcenter.service.RpcServerService;
import org.infinity.luix.webcenter.service.RpcServiceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcProviderProcessImpl implements GlobalProviderDiscoveryListener {

    @Resource
    private RpcProviderRepository rpcProviderRepository;
    @Resource
    private RpcServerService      rpcServerService;
    @Resource
    private RpcServiceService     rpcServiceService;
    @Resource
    private RpcApplicationService rpcApplicationService;

    @Override
    public void onNotify(Url registryUrl, String interfaceName, List<Url> providerUrls) {
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            for (Url providerUrl : providerUrls) {
                RpcProvider rpcProvider = RpcProvider.of(providerUrl, registryUrl);
                if (BuildInService.class.getName().equals(rpcProvider.getInterfaceName())) {
                    continue;
                }
                log.info("Discovered active providers: {}", providerUrl);
                // Insert or update provider
                rpcProviderRepository.save(rpcProvider);

                // Insert server
                rpcServerService.insert(registryUrl, providerUrl, rpcProvider.getAddress());

                // Insert service
                rpcServiceService.insert(registryUrl, rpcProvider.getInterfaceName());

                // Insert application
                rpcApplicationService.insert(registryUrl, providerUrl, rpcProvider.getApplication());
            }
        } else {
            // Update providers to inactive
            List<RpcProvider> list = rpcProviderRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            log.info("Discovered inactive providers of [{}]", interfaceName);

            list.forEach(provider -> provider.setActive(false));
            rpcProviderRepository.saveAll(list);
            rpcServerService.deactivate(list.get(0).getRegistryIdentity(), list.get(0).getAddress());
            rpcServiceService.deactivate(list.get(0).getRegistryIdentity(), list.get(0).getInterfaceName());
            rpcApplicationService.deactivate(list.get(0).getRegistryIdentity(), list.get(0).getApplication());
        }
    }
}
