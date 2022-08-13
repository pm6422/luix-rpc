package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.webcenter.domain.RpcConsumer;
import com.luixtech.rpc.webcenter.repository.RpcConsumerRepository;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

import static com.luixtech.rpc.webcenter.domain.RpcConsumer.*;
import static com.luixtech.rpc.webcenter.domain.RpcProvider.FIELD_ADDRESS;

@Service
@AllArgsConstructor
public class RpcConsumerServiceImpl implements RpcConsumerService {
    private final RpcConsumerRepository rpcConsumerRepository;
    private final MongoTemplate         mongoTemplate;

    @Override
    public Page<RpcConsumer> find(Pageable pageable, String registryIdentity,
                                  String application, String address, String interfaceName, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(application)) {
            query.addCriteria(Criteria.where(FIELD_APPLICATION).is(application));
        }
        if (StringUtils.isNotEmpty(address)) {
            query.addCriteria(Criteria.where(FIELD_ADDRESS).is(address));
        }
        if (StringUtils.isNotEmpty(interfaceName)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + interfaceName + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_INTERFACE_NAME).regex(pattern));
        }
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }
        long totalCount = mongoTemplate.count(query, RpcConsumer.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcConsumer.class), pageable, totalCount);
    }

    @Override
    public List<String> findDistinctApplications(String registryIdentity, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }
        return mongoTemplate.findDistinct(query, FIELD_APPLICATION, RpcConsumer.class, String.class);
    }

    @Override
    public boolean existsApplication(String registryIdentity, String application, boolean active) {
        return rpcConsumerRepository
                .existsByRegistryIdentityAndApplicationAndActive(registryIdentity, application, true);
    }

    @Override
    public boolean existsService(String registryIdentity, String interfaceName, boolean active) {
        return rpcConsumerRepository
                .existsByRegistryIdentityAndInterfaceNameAndActive(registryIdentity, interfaceName, true);
    }

    @Override
    public boolean existsAddress(String registryIdentity, String address, boolean active) {
        return rpcConsumerRepository.existsByRegistryIdentityAndAddressAndActive(registryIdentity, address, true);
    }
}
