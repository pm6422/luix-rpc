package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.domain.RpcServer;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.repository.RpcServerRepository;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcServerService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcProviderProcessImpl implements ProviderProcessable {

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

                // Insert server
                boolean existsServer = rpcServerService
                        .exists(rpcProvider.getRegistryIdentity(), rpcProvider.getAddress());
                if (!existsServer) {
                    RpcServer rpcServer = new RpcServer();
                    rpcServer.setRegistryIdentity(rpcProvider.getRegistryIdentity());
                    rpcServer.setAddress(rpcProvider.getAddress());
                    rpcServerRepository.save(rpcServer);
                }

                // Insert service
                boolean existsService = rpcServiceService
                        .exists(rpcProvider.getRegistryIdentity(), rpcProvider.getInterfaceName());
                if (!existsService) {
                    RpcService rpcService = new RpcService();
                    BeanUtils.copyProperties(rpcProvider, rpcService);
                    rpcService.setId(null);
                    rpcServiceRepository.save(rpcService);
                }

                // Insert application
                boolean existsApplication = rpcApplicationService
                        .exists(rpcProvider.getRegistryIdentity(), rpcProvider.getApplication());
                if (!existsApplication) {
                    RpcApplication remoteRpcApplication = rpcApplicationService.loadApplication(registryUrl, providerUrl);
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

            // Update service to inactive
            rpcServiceService.inactivate(list.get(0).getInterfaceName(), list.get(0).getRegistryIdentity());

            // Update application to inactive
            rpcApplicationService.inactivate(list.get(0).getApplication(), list.get(0).getRegistryIdentity());
        }
    }
}
