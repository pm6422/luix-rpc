package org.infinity.luix.webcenter.repository;

import org.infinity.luix.webcenter.domain.RpcApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the RpcApplication entity.
 */
@Repository
public interface RpcApplicationRepository extends MongoRepository<RpcApplication, String> {

    long countByActive(boolean active);

    Optional<RpcApplication> findByRegistryIdentityAndId(String registryIdentity, String application);

    List<RpcApplication> findByRegistryIdentity(String registryIdentity);
}


