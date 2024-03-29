package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.webcenter.domain.RpcProvider;
import com.luixtech.rpc.webcenter.repository.RpcProviderRepository;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

import static com.luixtech.rpc.webcenter.domain.RpcProvider.*;


@Service
@AllArgsConstructor
public class RpcProviderServiceImpl implements RpcProviderService {
    private final RpcProviderRepository rpcProviderRepository;
    private final MongoTemplate         mongoTemplate;

    @Override
    public Page<RpcProvider> find(Pageable pageable, String registryIdentity,
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

    @Override
    public boolean existsApplicationService(String registryIdentity, String interfaceName, boolean active) {
        List<RpcProvider> providers = rpcProviderRepository.findByInterfaceName(interfaceName);
        if (CollectionUtils.isEmpty(providers)) {
            return false;
        }
        return existsApplication(registryIdentity, providers.get(0).getApplication(), active);
    }

    @Override
    public void updateActiveByRegistryIdentityAndUrl(boolean active, String registryIdentity, String providerUrl) {
        Query query = new Query();
        query.addCriteria(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        query.addCriteria(Criteria.where(FIELD_URL).is(providerUrl));
        Update update = new Update();
        update.set(FIELD_ACTIVE, active);
        mongoTemplate.updateFirst(query, update, RpcProvider.class);
    }
}
