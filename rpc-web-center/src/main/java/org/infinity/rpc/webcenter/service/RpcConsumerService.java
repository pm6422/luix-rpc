package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RpcConsumerService {

    Page<RpcConsumer> find(Pageable pageable, String registryUrl, String application, String interfaceName, Boolean active);

    List<String> findDistinctApplications(String registryUrl, Boolean active);
}
