package org.infinity.rpc.democlient.service;

import org.infinity.rpc.democlient.domain.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProviderService {

    Page<Provider> find(Pageable pageable, String registryUrl, String application, String interfaceName, Boolean active);

    List<String> findDistinctApplications(String registryUrl, Boolean active);

    void insert(Provider provider);

    void update(Provider provider);

    void deleteById(String id);

}
