package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.core.listener.GlobalProviderDiscoveryListener;
import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.webcenter.domain.RpcProvider;
import com.luixtech.rpc.webcenter.repository.RpcProviderRepository;
import com.luixtech.rpc.webcenter.service.RpcApplicationService;
import com.luixtech.rpc.webcenter.service.RpcServerService;
import com.luixtech.rpc.webcenter.service.RpcServiceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class RpcProviderProcessImpl implements GlobalProviderDiscoveryListener {
    private final RpcProviderRepository rpcProviderRepository;
    private final RpcServerService      rpcServerService;
    private final RpcServiceService     rpcServiceService;
    private final RpcApplicationService rpcApplicationService;

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
        }
    }
}
