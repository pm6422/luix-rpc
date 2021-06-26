package org.infinity.rpc.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcConsumerService;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Pattern;

import static org.infinity.rpc.webcenter.domain.RpcService.FIELD_INTERFACE_NAME;
import static org.infinity.rpc.webcenter.domain.RpcService.FIELD_REGISTRY_IDENTITY;

@Service
public class RpcServiceServiceImpl implements RpcServiceService {

    @Resource
    private MongoTemplate        mongoTemplate;
    @Resource
    private RpcServiceRepository rpcServiceRepository;
    @Resource
    private RpcProviderService   rpcProviderService;
    @Resource
    private RpcConsumerService   rpcConsumerService;

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
