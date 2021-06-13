package org.infinity.rpc.democlient.service;

import org.infinity.rpc.democlient.domain.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ConsumerService {

    Page<Consumer> find(Pageable pageable, String registryUrl, String application, String interfaceName, Boolean active);

    List<String> findDistinctApplications(String registryUrl, Boolean active);
}
