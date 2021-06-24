package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcProviderProcessImpl implements ProviderProcessable {

    @Resource
    private RpcProviderRepository    rpcProviderRepository;
    @Resource
    private RpcServiceRepository     rpcServiceRepository;
    @Resource
    private RpcApplicationRepository rpcApplicationRepository;
    @Resource
    private RpcServiceService        rpcServiceService;
    @Resource
    private RpcApplicationService    rpcApplicationService;

    @Override
    public void process(Url registryUrl, String interfaceName, List<Url> providerUrls) {
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            log.info("Discovered active providers {}", providerUrls);
            for (Url providerUrl : providerUrls) {
                RpcProvider rpcProvider = RpcProvider.of(providerUrl, registryUrl);
                // Insert or update provider
                rpcProviderRepository.save(rpcProvider);

                RpcService rpcService = new RpcService();
                BeanUtils.copyProperties(rpcProvider, rpcService);
                rpcService.setProviding(true);
                rpcServiceRepository.save(rpcService);

                // Insert application
                RpcApplication probe = new RpcApplication();
                probe.setName(rpcProvider.getApplication());
                probe.setRegistryIdentity(rpcProvider.getRegistryIdentity());
                // Ignore query parameter if it has a null value
                ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
                if (rpcApplicationRepository.exists(Example.of(probe, matcher))) {
                    // If application exists
                    continue;
                }

                RpcApplication rpcApplication = rpcApplicationService.remoteQueryApplication(registryUrl, providerUrl);
                rpcApplicationRepository.save(rpcApplication);
            }
        } else {
            log.info("Discovered offline providers of [{}]", interfaceName);

            // Update providers to inactive
            List<RpcProvider> list = rpcProviderRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            list.forEach(provider -> provider.setActive(false));
            rpcProviderRepository.saveAll(list);

            // Update providing flag
            rpcServiceService.inactivate(list.get(0).getInterfaceName(), list.get(0).getRegistryIdentity());

            // Update application to inactive
            rpcApplicationService.inactivate(list.get(0).getApplication(), list.get(0).getRegistryIdentity());
        }
    }
}
