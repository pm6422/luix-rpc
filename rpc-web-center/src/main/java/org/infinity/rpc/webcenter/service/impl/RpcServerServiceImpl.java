package org.infinity.rpc.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.domain.RpcServer;
import org.infinity.rpc.webcenter.repository.RpcServerRepository;
import org.infinity.rpc.webcenter.service.RpcConsumerService;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcServerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Pattern;

import static org.infinity.rpc.webcenter.domain.RpcServer.FIELD_ADDRESS;
import static org.infinity.rpc.webcenter.domain.RpcServer.FIELD_REGISTRY_IDENTITY;

@Service
public class RpcServerServiceImpl implements RpcServerService {

    @Resource
    private MongoTemplate       mongoTemplate;
    @Resource
    private RpcServerRepository rpcServerRepository;
    @Resource
    private RpcProviderService  rpcProviderService;
    @Resource
    private RpcConsumerService  rpcConsumerService;

    @Override
    public boolean exists(String registryIdentity, String address) {
        return rpcServerRepository.existsByRegistryIdentityAndAddress(registryIdentity, address);
    }

    @Override
    public Page<RpcServer> find(Pageable pageable, String registryIdentity, String address) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(address)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + address + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_ADDRESS).regex(pattern));
        }
        long totalCount = mongoTemplate.count(query, RpcServer.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcServer.class), pageable, totalCount);
    }

    @Override
    public void inactivate(String registryIdentity, String address) {
        if (!rpcProviderService.existsService(registryIdentity, address, true)
                && !rpcConsumerService.existsService(registryIdentity, address, true)) {
            RpcServer rpcServer = rpcServerRepository.findByRegistryIdentityAndAddress(registryIdentity, address);
            if (rpcServer == null) {
                return;
            }
            rpcServer.setActive(false);
            rpcServerRepository.save(rpcServer);
        }
    }
}
