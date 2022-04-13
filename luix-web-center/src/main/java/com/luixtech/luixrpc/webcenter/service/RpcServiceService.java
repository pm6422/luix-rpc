package com.luixtech.luixrpc.webcenter.service;

import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.webcenter.domain.RpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcServiceService {

    void updateStatus();

    boolean exists(String registryIdentity, String interfaceName);

    Page<RpcService> find(Pageable pageable, String registryIdentity, String interfaceName);

    void insert(Url registryUrl, String interfaceName);

    void deactivate(String registryIdentity, String interfaceName);
}
