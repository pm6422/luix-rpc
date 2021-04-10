package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.service.ProviderService;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.infinity.rpc.core.constant.ApplicationConstants.APP;

@Service
@Slf4j
public class ProviderProcessServiceImpl implements ProviderProcessable {

    private final ProviderService providerService;

    public ProviderProcessServiceImpl(ProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public void process(Url registryUrl, List<Url> providerUrls, String interfaceName) {
        if (CollectionUtils.isEmpty(providerUrls)) {
            log.info("Discovered active providers [{}]", providerUrls);
            for (Url providerUrl : providerUrls) {
                Provider provider = new Provider();
                provider.setId(String.valueOf(IdGenerator.generateShortId()));
                provider.setInterfaceName(providerUrl.getPath());
                provider.setForm(providerUrl.getForm());
                provider.setVersion(providerUrl.getVersion());
                provider.setApplication(providerUrl.getOption(APP));
                provider.setHost(providerUrl.getHost());
                provider.setAddress(providerUrl.getAddress());
                provider.setRegistryUrl(registryUrl.toFullStr());
                provider.setActive(true);

                providerService.insert(provider);
            }
        } else {
            log.info("Discovered inactive providers of [{}]", interfaceName);
        }
    }
}
