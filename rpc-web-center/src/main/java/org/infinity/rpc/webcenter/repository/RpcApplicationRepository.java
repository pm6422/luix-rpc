package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the RpcApplication entity.
 */
@Repository
public interface RpcApplicationRepository extends MongoRepository<RpcApplication, String> {

    boolean existsByRegistryIdentityAndName(String registryIdentity, String application);

    Optional<RpcApplication> findByRegistryIdentityAndName(String registryIdentity, String application);

    List<RpcApplication> findByRegistryIdentity(String registryIdentity);
}


