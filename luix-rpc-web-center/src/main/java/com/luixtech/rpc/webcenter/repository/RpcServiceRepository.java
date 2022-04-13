package com.luixtech.rpc.webcenter.repository;

import com.luixtech.rpc.webcenter.domain.RpcService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the RpcService entity.
 */
@Repository
public interface RpcServiceRepository extends MongoRepository<RpcService, String> {

    long countByActive(boolean active);

    boolean existsByRegistryIdentityAndInterfaceName(String registryIdentity, String interfaceName);

    RpcService findByRegistryIdentityAndInterfaceName(String registryIdentity, String interfaceName);
}
