package org.infinity.rpc.democlient.repository;

import org.infinity.rpc.democlient.domain.Provider;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Provider entity.
 */
@Repository
public interface ProviderRepository extends MongoRepository<Provider, String> {

    List<Provider> findByInterfaceName(String interfaceName);

    int countByApplicationAndRegistryIdentityAndActiveIsTrue(String application, String registryIdentity);
}
