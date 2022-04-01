package org.infinity.luix.webcenter.service;

import org.infinity.luix.core.url.Url;
import org.infinity.luix.webcenter.domain.RpcApplication;
import org.infinity.luix.webcenter.domain.RpcConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcApplicationService {

    void updateStatus();

    Page<RpcApplication> find(Pageable pageable, String registryIdentity, String name, Boolean active);

    void insert(Url registryUrl, Url consumerUrl, String id);

    void inactivate(String registryIdentity, String name);

    RpcApplication loadApplication(Url registryUrl, Url url);
}
