package org.infinity.rpc.democlient.service;

import org.infinity.rpc.democlient.domain.Provider;

public interface ProviderService {

    void insert(Provider provider);

    void update(Provider provider);

    void deleteById(String id);
}
