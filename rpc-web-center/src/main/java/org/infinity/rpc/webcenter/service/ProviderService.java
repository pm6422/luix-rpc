package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProviderService {

    Page<Provider> find(Pageable pageable, String registryUrl, String application, String interfaceName, Boolean active);

    List<String> findDistinctApplications(String registryUrl, Boolean active);
}
