package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the RpcService entity.
 */
@Repository
public interface RpcServiceRepository extends MongoRepository<RpcService, String> {

    boolean existsByRegistryIdentityAndInterfaceName(String registryIdentity, String interfaceName);

    RpcService findByRegistryIdentityAndInterfaceName(String registryIdentity, String interfaceName);
}
