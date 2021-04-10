package org.infinity.rpc.democlient.service;

import org.infinity.rpc.democlient.domain.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProviderService {

    List<String> findDistinctApplicationByRegistryUrl(String registryUrl);

    Page<Provider> findByRegistryUrlAndApplicationAndInterfaceName(Pageable pageable, String registryUrl,
                                                                   String application, String interfaceName);

    void insert(Provider provider);

    void update(Provider provider);

    void deleteById(String id);

}
