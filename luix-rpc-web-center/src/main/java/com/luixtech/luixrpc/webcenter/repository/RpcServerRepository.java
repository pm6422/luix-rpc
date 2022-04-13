package com.luixtech.luixrpc.webcenter.repository;

import com.luixtech.luixrpc.webcenter.domain.RpcServer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the RpcServer entity.
 */
@Repository
public interface RpcServerRepository extends MongoRepository<RpcServer, String> {

    long countByActive(boolean active);

    boolean existsByRegistryIdentityAndAddress(String registryIdentity, String address);

    RpcServer findByRegistryIdentityAndAddress(String registryIdentity, String address);
}
