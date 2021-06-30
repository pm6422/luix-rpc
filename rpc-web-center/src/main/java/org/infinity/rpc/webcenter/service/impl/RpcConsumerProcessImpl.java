package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.listener.ConsumerProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.infinity.rpc.webcenter.domain.RpcServer;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.repository.RpcConsumerRepository;
import org.infinity.rpc.webcenter.repository.RpcServerRepository;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcServerService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RpcConsumerProcessImpl implements ConsumerProcessable {

    @Resource
    private RpcConsumerRepository    rpcConsumerRepository;
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
    public void process(Url registryUrl, String interfaceName, List<Url> consumerUrls) {
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
                boolean existsServer = rpcServerService
                        .exists(rpcConsumer.getRegistryIdentity(), rpcConsumer.getAddress());
                if (!existsServer) {
                    RpcServer rpcServer = new RpcServer();
                    rpcServer.setRegistryIdentity(rpcConsumer.getRegistryIdentity());
                    rpcServer.setAddress(rpcConsumer.getAddress());
                    rpcServerRepository.save(rpcServer);
                }

                // Insert or update service
                RpcService rpcService = RpcService.of(rpcConsumer.getInterfaceName(), registryUrl);
                rpcServiceRepository.save(rpcService);

                // Insert application
                boolean existsApplication = rpcApplicationService
                        .exists(rpcConsumer.getRegistryIdentity(), rpcConsumer.getApplication());
                if (!existsApplication) {
                    RpcApplication remoteRpcApplication = rpcApplicationService.loadApplication(registryUrl, consumerUrl);
                    rpcApplicationRepository.save(remoteRpcApplication);
                }
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
