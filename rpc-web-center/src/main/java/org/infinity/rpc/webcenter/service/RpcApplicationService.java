package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcApplicationService {

    boolean exists(String registryIdentity, String name);

    Page<RpcApplication> find(Pageable pageable, String registryIdentity, String name, Boolean active);

    void inactivate(String registryIdentity, String name);

    RpcApplication loadApplication(Url registryUrl, Url url);
}
