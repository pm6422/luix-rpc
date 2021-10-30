package org.infinity.luix.webcenter.service;

import org.infinity.luix.webcenter.domain.RpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcServiceService {

    void updateStatus();

    boolean exists(String registryIdentity, String interfaceName);

    Page<RpcService> find(Pageable pageable, String registryIdentity, String interfaceName);

    void inactivate(String registryIdentity, String interfaceName);
}
