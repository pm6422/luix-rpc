package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the RpcConsumer entity.
 */
@Repository
public interface RpcConsumerRepository extends MongoRepository<RpcConsumer, String> {

    List<RpcConsumer> findByInterfaceName(String interfaceName);

    boolean existsByRegistryIdentityAndApplicationAndActive(String registryIdentity, String application, boolean active);

    boolean existsByRegistryIdentityAndInterfaceNameAndActive(String registryIdentity, String interfaceName, boolean active);

    boolean existsByRegistryIdentityAndAddressAndActive(String registryIdentity, String address, boolean active);
}
