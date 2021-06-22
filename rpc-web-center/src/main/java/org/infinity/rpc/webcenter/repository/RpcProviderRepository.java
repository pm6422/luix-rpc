package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the RpcProvider entity.
 */
@Repository
public interface RpcProviderRepository extends MongoRepository<RpcProvider, String> {

    List<RpcProvider> findByInterfaceName(String interfaceName);
}
