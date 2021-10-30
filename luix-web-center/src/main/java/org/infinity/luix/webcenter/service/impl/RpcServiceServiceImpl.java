package org.infinity.luix.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.webcenter.domain.RpcService;
import org.infinity.luix.webcenter.repository.RpcServiceRepository;
import org.infinity.luix.webcenter.service.RpcConsumerService;
import org.infinity.luix.webcenter.service.RpcProviderService;
import org.infinity.luix.webcenter.service.RpcServiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.util.regex.Pattern;

import static org.infinity.luix.webcenter.domain.RpcService.FIELD_INTERFACE_NAME;
import static org.infinity.luix.webcenter.domain.RpcService.FIELD_REGISTRY_IDENTITY;

@RpcProvider
public class RpcServiceServiceImpl implements RpcServiceService {

    private static final int                  PAGE_SIZE = 100;
    @Resource
    private              MongoTemplate        mongoTemplate;
    @Resource
    private              RpcServiceRepository rpcServiceRepository;
    @Resource
    private              RpcProviderService   rpcProviderService;
    @Resource
    private              RpcConsumerService   rpcConsumerService;

    @Override
    public void updateStatus() {
        long total = rpcServiceRepository.count();
        long loopCount = total % PAGE_SIZE == 0 ? total / PAGE_SIZE : total / PAGE_SIZE + 1;
        for (int i = 0; i < loopCount; i++) {
            Pageable pageable = PageRequest.of(i, PAGE_SIZE);
            Page<RpcService> services = rpcServiceRepository.findAll(pageable);
            if (services.isEmpty()) {
                return;
            }
            services.getContent().forEach(domain -> {
                if (rpcProviderService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
                    domain.setProviding(true);
                    domain.setActive(true);
                }
                if (rpcConsumerService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
                    domain.setConsuming(true);
                    domain.setActive(true);
                }
            });
            rpcServiceRepository.saveAll(services);
        }
    }

    @Override
    public boolean exists(String registryIdentity, String interfaceName) {
        return rpcServiceRepository.existsByRegistryIdentityAndInterfaceName(registryIdentity, interfaceName);
    }

    @Override
    public Page<RpcService> find(Pageable pageable, String registryIdentity, String interfaceName) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(interfaceName)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + interfaceName + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_INTERFACE_NAME).regex(pattern));
        }
        long totalCount = mongoTemplate.count(query, RpcService.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcService.class), pageable, totalCount);
    }

    @Override
    public void inactivate(String registryIdentity, String interfaceName) {
        if (!rpcProviderService.existsService(registryIdentity, interfaceName, true)
                && !rpcConsumerService.existsService(registryIdentity, interfaceName, true)) {
            RpcService rpcService = rpcServiceRepository
                    .findByRegistryIdentityAndInterfaceName(registryIdentity, interfaceName);
            if (rpcService == null) {
                return;
            }
            rpcService.setActive(false);
            rpcServiceRepository.save(rpcService);
        }
    }
}
