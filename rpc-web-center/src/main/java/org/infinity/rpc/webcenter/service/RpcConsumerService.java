package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RpcConsumerService {

    Page<RpcConsumer> find(Pageable pageable, String registryIdentity, String application, String interfaceName, Boolean active);

    List<String> findDistinctApplications(String registryIdentity, Boolean active);

    boolean existsApplication(String registryIdentity, String application, boolean active);

    boolean existsService(String registryIdentity, String interfaceName, boolean active);

    boolean existsAddress(String registryIdentity, String address, boolean active);
}
