package org.infinity.rpc.democlient.repository;

import org.infinity.rpc.democlient.domain.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the Application entity.
 */
@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {

    Optional<Application> findByNameAndRegistryIdentity(String application, String registryIdentity);
}


