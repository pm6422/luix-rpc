package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcApplicationService {

    Page<RpcApplication> find(Pageable pageable, String registryUrl, String name, Boolean active);

    RpcApplication remoteQueryApplication(Url registryUrl, Url url);

    void inactivate(String applicationName, String registryIdentity);
}
