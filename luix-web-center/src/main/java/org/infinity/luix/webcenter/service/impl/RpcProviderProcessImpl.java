package org.infinity.luix.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.core.listener.client.ConsumersListener;
import org.infinity.luix.core.server.buildin.BuildInService;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.webcenter.domain.RpcApplication;
import org.infinity.luix.webcenter.domain.RpcProvider;
import org.infinity.luix.webcenter.domain.RpcServer;
import org.infinity.luix.webcenter.domain.RpcService;
import org.infinity.luix.webcenter.repository.RpcApplicationRepository;
import org.infinity.luix.webcenter.repository.RpcProviderRepository;
import org.infinity.luix.webcenter.repository.RpcServerRepository;
import org.infinity.luix.webcenter.repository.RpcServiceRepository;
import org.infinity.luix.webcenter.service.RpcApplicationService;
import org.infinity.luix.webcenter.service.RpcServerService;
import org.infinity.luix.webcenter.service.RpcServiceService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static org.infinity.luix.webcenter.domain.RpcService.generateMd5Id;

@Service
@Slf4j
public class RpcProviderProcessImpl implements ConsumersListener {

    @Resource
    private RpcProviderRepository    rpcProviderRepository;
    @Resource
    private RpcServerRepository      rpcServerRepository;
    @Resource
    private RpcServiceRepository     rpcServiceRepository;
    @Resource
    private RpcApplicationRepository rpcApplicationRepository;
    @Resource
    private RpcServerService         rpcServerService;
    @Resource
    private RpcServiceService        rpcServiceService;
    @Resource
    private RpcApplicationService    rpcApplicationService;

    @Override
    public void onNotify(Url registryUrl, String interfaceName, List<Url> providerUrls) {
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            log.info("Discovered active providers {}", providerUrls);
            for (Url providerUrl : providerUrls) {
                RpcProvider rpcProvider = RpcProvider.of(providerUrl, registryUrl);
                if (BuildInService.class.getName().equals(rpcProvider.getInterfaceName())) {
                    continue;
                }
                // Insert or update provider
                rpcProviderRepository.save(rpcProvider);

                // Insert server
                insertServer(registryUrl, providerUrl, rpcProvider);

                // Insert service
                insertService(registryUrl, rpcProvider);

                // Insert application
                insertApplication(registryUrl, providerUrl, rpcProvider);
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

            // Update service to inactive
            rpcServiceService.inactivate(list.get(0).getInterfaceName(), list.get(0).getRegistryIdentity());

            // Update application to inactive
            rpcApplicationService.inactivate(list.get(0).getApplication(), list.get(0).getRegistryIdentity());
        }
    }

    private void insertServer(Url registryUrl, Url providerUrl, RpcProvider rpcProvider) {
        if (!rpcServerRepository.existsById(generateMd5Id(rpcProvider.getAddress(), registryUrl.getIdentity()))) {
            RpcServer rpcServer = rpcServerService.loadServer(registryUrl, providerUrl);
            try {
                rpcServerRepository.insert(rpcServer);
            } catch (DuplicateKeyException ex) {
                log.warn("Ignore the duplicated index issue!");
            }
        }
    }

    private synchronized void insertService(Url registryUrl, RpcProvider rpcProvider) {
        if (!rpcServiceRepository.existsById(generateMd5Id(rpcProvider.getInterfaceName(), registryUrl.getIdentity()))) {
            RpcService rpcService = RpcService.of(rpcProvider.getInterfaceName(), registryUrl);
            try {
                rpcServiceRepository.insert(rpcService);
            } catch (DuplicateKeyException ex) {
                log.warn("Ignore the duplicated index issue!");
            }
        }
    }

    private void insertApplication(Url registryUrl, Url providerUrl, RpcProvider rpcProvider) {
        if (!rpcApplicationRepository.existsById(generateMd5Id(rpcProvider.getApplication(), registryUrl.getIdentity()))) {
            RpcApplication rpcApplication = rpcApplicationService.loadApplication(registryUrl, providerUrl);
            try {
                rpcApplicationRepository.insert(rpcApplication);
            } catch (DuplicateKeyException ex) {
                log.warn("Ignore the duplicated index issue!");
            }
        }
    }
}
