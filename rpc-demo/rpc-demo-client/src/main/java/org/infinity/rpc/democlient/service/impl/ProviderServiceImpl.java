package org.infinity.rpc.democlient.service.impl;

import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.repository.ProviderRepository;
import org.infinity.rpc.democlient.service.ProviderService;
import org.springframework.stereotype.Service;

@Service
public class ProviderServiceImpl implements ProviderService {
    private final ProviderRepository providerRepository;

    public ProviderServiceImpl(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    public void insert(Provider provider) {
        providerRepository.save(provider);
    }

    @Override
    public void update(Provider provider) {

    }

    @Override
    public void deleteById(String id) {

    }
}
