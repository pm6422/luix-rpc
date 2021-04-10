package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.name.ProviderStubBeanNameBuilder;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.repository.ProviderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.infinity.rpc.core.constant.ApplicationConstants.APP;

@Service
@Slf4j
public class ProviderProcessServiceImpl implements ProviderProcessable {

    private final ProviderRepository providerRepository;

    public ProviderProcessServiceImpl(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    public void process(Url registryUrl, List<Url> providerUrls, String interfaceName) {
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            log.info("Discovered active providers [{}]", providerUrls);
            for (Url providerUrl : providerUrls) {
                Provider provider = new Provider();
                String id = ProviderStubBeanNameBuilder
                        .builder(providerUrl.getPath())
                        .form(providerUrl.getForm())
                        .version(providerUrl.getVersion())
                        .build();
                provider.setId(id);
                provider.setInterfaceName(providerUrl.getPath());
                provider.setForm(providerUrl.getForm());
                provider.setVersion(providerUrl.getVersion());
                provider.setApplication(providerUrl.getOption(APP));
                provider.setHost(providerUrl.getHost());
                provider.setAddress(providerUrl.getAddress());
                provider.setProviderUrl(providerUrl.toFullStr());
                provider.setRegistryUrl(registryUrl.getIdentity());
                provider.setActive(true);

                providerRepository.save(provider);
            }
        } else {
            log.info("Discovered inactive providers of [{}]", interfaceName);
        }
    }
}
