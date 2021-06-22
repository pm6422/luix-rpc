package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the RpcApplication entity.
 */
@Repository
public interface RpcApplicationRepository extends MongoRepository<RpcApplication, String> {

    Optional<RpcApplication> findByNameAndRegistryIdentity(String application, String registryIdentity);
}


