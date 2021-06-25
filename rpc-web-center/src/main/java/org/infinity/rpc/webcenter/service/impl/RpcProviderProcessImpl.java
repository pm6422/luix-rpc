package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.repository.RpcConsumerRepository;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class RpcProviderProcessImpl implements ProviderProcessable {

    @Resource
    private RpcProviderRepository    rpcProviderRepository;
    @Resource
    private RpcConsumerRepository    rpcConsumerRepository;
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
                if (BuildInService.class.getName().equals(rpcProvider.getInterfaceName())) {
                    continue;
                }
                // Insert or update provider
                rpcProviderRepository.save(rpcProvider);

                // Insert or update service
                Optional<RpcService> existingRpcService = rpcServiceRepository
                        .findByInterfaceNameAndRegistryIdentity(rpcProvider.getInterfaceName(), rpcProvider.getRegistryIdentity());
                if (existingRpcService.isPresent()) {
                    // Update
                    existingRpcService.get().setProviding(true);
                    if (rpcConsumerRepository.countByInterfaceName(rpcProvider.getInterfaceName()) > 0) {
                        existingRpcService.get().setConsuming(true);
                    }
                    rpcServiceRepository.save(existingRpcService.get());
                } else {
                    // Insert
                    RpcService rpcService = new RpcService();
                    BeanUtils.copyProperties(rpcProvider, rpcService);
                    rpcService.setId(null);
                    rpcService.setProviding(true);
                    if (rpcConsumerRepository.countByInterfaceName(rpcProvider.getInterfaceName()) > 0) {
                        rpcService.setConsuming(true);
                    }
                    rpcServiceRepository.save(rpcService);
                }

                // Insert or update application
                Optional<RpcApplication> rpcApplication = rpcApplicationRepository
                        .findByNameAndRegistryIdentity(rpcProvider.getApplication(), rpcProvider.getRegistryIdentity());
                if (rpcApplication.isPresent()) {
                    // Update
                    rpcApplication.get().setProviding(true);
                    if (rpcConsumerRepository.countByInterfaceName(rpcProvider.getInterfaceName()) > 0) {
                        rpcApplication.get().setConsuming(true);
                    }
                    rpcApplicationRepository.save(rpcApplication.get());
                } else {
                    // Insert
                    RpcApplication remoteRpcApplication = rpcApplicationService.remoteQueryApplication(registryUrl, providerUrl);
                    remoteRpcApplication.setProviding(true);
                    if (rpcConsumerRepository.countByInterfaceName(rpcProvider.getInterfaceName()) > 0) {
                        remoteRpcApplication.setConsuming(true);
                    }
                    rpcApplicationRepository.save(remoteRpcApplication);
                }
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
