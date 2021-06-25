package org.infinity.rpc.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.repository.RpcConsumerRepository;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.infinity.rpc.webcenter.domain.RpcService.*;

@Service
public class RpcServiceServiceImpl implements RpcServiceService {

    @Resource
    private MongoTemplate         mongoTemplate;
    @Resource
    private RpcProviderRepository rpcProviderRepository;
    @Resource
    private RpcConsumerRepository rpcConsumerRepository;
    @Resource
    private RpcServiceRepository  rpcServiceRepository;

    @Override
    public Page<RpcService> find(Pageable pageable, String registryIdentity, String interfaceName,
                                 Boolean providing, Boolean consuming) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(interfaceName)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + interfaceName + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_INTERFACE_NAME).regex(pattern));
        }
        if (providing != null) {
            query.addCriteria(Criteria.where(FIELD_PROVIDING).is(providing));
        }
        if (consuming != null) {
            query.addCriteria(Criteria.where(FIELD_CONSUMING).is(consuming));
        }
        long totalCount = mongoTemplate.count(query, RpcService.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcService.class), pageable, totalCount);
    }

    @Override
    public void inactivate(String interfaceName, String registryIdentity) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();

        RpcProvider rpcProviderProbe = new RpcProvider();
        rpcProviderProbe.setInterfaceName(interfaceName);
        rpcProviderProbe.setRegistryIdentity(registryIdentity);
        rpcProviderProbe.setActive(true);

        if (!rpcProviderRepository.exists(Example.of(rpcProviderProbe, matcher))) {
            Optional<RpcService> rpcService = rpcServiceRepository.findByInterfaceNameAndRegistryIdentity(interfaceName,
                    registryIdentity);
            if (!rpcService.isPresent()) {
                return;
            }
            rpcService.get().setProviding(false);
            rpcServiceRepository.save(rpcService.get());
        }

        RpcConsumer rpcConsumerProbe = new RpcConsumer();
        rpcConsumerProbe.setInterfaceName(interfaceName);
        rpcConsumerProbe.setRegistryIdentity(registryIdentity);
        rpcConsumerProbe.setActive(true);

        if (!rpcConsumerRepository.exists(Example.of(rpcConsumerProbe, matcher))) {
            Optional<RpcService> rpcService = rpcServiceRepository.findByInterfaceNameAndRegistryIdentity(interfaceName,
                    registryIdentity);
            if (!rpcService.isPresent()) {
                return;
            }
            rpcService.get().setConsuming(false);
            rpcServiceRepository.save(rpcService.get());
        }
    }
}
