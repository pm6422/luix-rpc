package org.infinity.luix.webcenter.service;

import org.infinity.luix.core.url.Url;
import org.infinity.luix.webcenter.domain.RpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcServiceService {

    void updateStatus();

    boolean exists(String registryIdentity, String interfaceName);

    Page<RpcService> find(Pageable pageable, String registryIdentity, String interfaceName);

    void insert(Url registryUrl, String interfaceName);

    void deactivate(String registryIdentity, String interfaceName);
}
