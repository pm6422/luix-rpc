package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.listener.ConsumerProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.*;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.repository.RpcConsumerRepository;
import org.infinity.rpc.webcenter.repository.RpcServerRepository;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcServerService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static org.infinity.rpc.webcenter.domain.RpcService.generateMd5Id;

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
                insertServer(registryUrl, consumerUrl, rpcConsumer);

                // Insert service
                insertService(registryUrl, rpcConsumer);

                // Insert application
                insertApplication(registryUrl, consumerUrl, rpcConsumer);
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

    private void insertServer(Url registryUrl, Url consumerUrl, RpcConsumer rpcConsumer) {
        if (!rpcServerRepository.existsById(generateMd5Id(rpcConsumer.getAddress(), registryUrl.getIdentity()))) {
            RpcServer rpcServer = rpcServerService.loadServer(registryUrl, consumerUrl);
            try {
                rpcServerRepository.insert(rpcServer);
            } catch (DuplicateKeyException ex) {
                log.warn("Ignore the duplicated index issue!");
            }
        }
    }

    private synchronized void insertService(Url registryUrl, RpcConsumer rpcConsumer) {
        if (!rpcServiceRepository.existsById(generateMd5Id(rpcConsumer.getInterfaceName(), registryUrl.getIdentity()))) {
            RpcService rpcService = RpcService.of(rpcConsumer.getInterfaceName(), registryUrl);
            try {
                rpcServiceRepository.insert(rpcService);
            } catch (DuplicateKeyException ex) {
                log.warn("Ignore the duplicated index issue!");
            }
        }
    }

    private void insertApplication(Url registryUrl, Url consumerUrl, RpcConsumer rpcConsumer) {
        if (!rpcApplicationRepository.existsById(generateMd5Id(rpcConsumer.getApplication(), registryUrl.getIdentity()))) {
            RpcApplication rpcApplication = rpcApplicationService.loadApplication(registryUrl, consumerUrl);
            try {
                rpcApplicationRepository.insert(rpcApplication);
            } catch (DuplicateKeyException ex) {
                log.warn("Ignore the duplicated index issue!");
            }
        }
    }
}
