package com.luixtech.luixrpc.webcenter.service;

import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.webcenter.domain.RpcServer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RpcServerService {

    void loadAll();

    void updateStatus();

    boolean exists(String registryIdentity, String address);

    Page<RpcServer> find(Pageable pageable, String registryIdentity, String address);

    void insert(Url registryUrl, Url url, String address);

    void deactivate(String registryIdentity, String address);

    RpcServer loadServer(Url registryUrl, Url url);

    RpcServer loadServer(String registryIdentity, String address);
}
