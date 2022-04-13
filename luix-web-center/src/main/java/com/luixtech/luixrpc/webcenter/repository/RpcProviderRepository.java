package com.luixtech.luixrpc.webcenter.repository;

import com.luixtech.luixrpc.webcenter.domain.RpcProvider;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the RpcProvider entity.
 */
@Repository
public interface RpcProviderRepository extends MongoRepository<RpcProvider, String> {

    long countByActive(boolean active);

    List<RpcProvider> findByInterfaceName(String interfaceName);

    List<RpcProvider> findByRegistryIdentityAndInterfaceNameAndActive(String registryIdentity, String interfaceName, boolean active);

    boolean existsByRegistryIdentityAndApplicationAndActive(String registryIdentity, String application, boolean active);

    boolean existsByRegistryIdentityAndInterfaceNameAndActive(String registryIdentity, String interfaceName, boolean active);

    boolean existsByRegistryIdentityAndAddressAndActive(String registryIdentity, String address, boolean active);
}
