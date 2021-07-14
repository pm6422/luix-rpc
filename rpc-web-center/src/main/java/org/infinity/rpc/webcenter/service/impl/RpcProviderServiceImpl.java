package org.infinity.rpc.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Pattern;

import static org.infinity.rpc.webcenter.domain.RpcProvider.*;


@Service
public class RpcProviderServiceImpl implements RpcProviderService {

    @Resource
    private RpcProviderRepository rpcProviderRepository;
    @Resource
    private MongoTemplate         mongoTemplate;

    @Override
    public Page<RpcProvider> find(Pageable pageable, String registryIdentity,
                                  String application, String interfaceName, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(application)) {
            query.addCriteria(Criteria.where(FIELD_APPLICATION).is(application));
        }
        if (StringUtils.isNotEmpty(interfaceName)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + interfaceName + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_INTERFACE_NAME).regex(pattern));
        }
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }
        long totalCount = mongoTemplate.count(query, RpcProvider.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcProvider.class), pageable, totalCount);
    }

    @Override
    public List<RpcProvider> find(String registryIdentity, String interfaceName, Boolean active) {
        return rpcProviderRepository.findByRegistryIdentityAndInterfaceNameAndActive(registryIdentity, interfaceName, active);
    }

    @Override
    public List<String> findDistinctApplications(String registryIdentity, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }
        return mongoTemplate.findDistinct(query, FIELD_APPLICATION, RpcProvider.class, String.class);
    }

    @Override
    public boolean existsApplication(String registryIdentity, String application, boolean active) {
        return rpcProviderRepository
                .existsByRegistryIdentityAndApplicationAndActive(registryIdentity, application, true);
    }

    @Override
    public boolean existsService(String registryIdentity, String interfaceName, boolean active) {
        return rpcProviderRepository
                .existsByRegistryIdentityAndInterfaceNameAndActive(registryIdentity, interfaceName, true);
    }

    @Override
    public boolean existsAddress(String registryIdentity, String address, boolean active) {
        return rpcProviderRepository.existsByRegistryIdentityAndAddressAndActive(registryIdentity, address, true);
    }
}
