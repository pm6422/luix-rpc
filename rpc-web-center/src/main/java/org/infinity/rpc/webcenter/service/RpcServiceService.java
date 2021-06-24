package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.RpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcServiceService {

    Page<RpcService> find(Pageable pageable, String registryIdentity, String application, String interfaceName,
                          Boolean providing, Boolean consuming);

    void inactivate(String interfaceName, String registryIdentity);
}
