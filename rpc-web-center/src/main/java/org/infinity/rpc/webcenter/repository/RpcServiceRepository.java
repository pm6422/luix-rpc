package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the RpcService entity.
 */
@Repository
public interface RpcServiceRepository extends MongoRepository<RpcService, String> {

    Optional<RpcService> findByInterfaceNameAndRegistryIdentity(String interfaceName, String registryIdentity);
}
