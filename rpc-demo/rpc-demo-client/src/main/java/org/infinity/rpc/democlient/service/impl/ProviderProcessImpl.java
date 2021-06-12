package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.repository.ApplicationRepository;
import org.infinity.rpc.democlient.repository.ProviderRepository;
import org.infinity.rpc.democlient.service.ApplicationService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class ProviderProcessImpl implements ProviderProcessable {

    @Resource
    private ProviderRepository    providerRepository;
    @Resource
    private ApplicationRepository applicationRepository;
    @Resource
    private ApplicationService    applicationService;

    @Override
    public void process(Url registryUrl, List<Url> providerUrls, String interfaceName) {
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            log.info("Discovered active providers {}", providerUrls);
            for (Url providerUrl : providerUrls) {
                Provider provider = Provider.of(providerUrl, registryUrl);
                // Insert or update provider
                providerRepository.save(provider);

                // Insert application
                Application probe = new Application();
                probe.setName(provider.getApplication());
                probe.setRegistryIdentity(provider.getRegistryIdentity());
                // Ignore query parameter if it has a null value
                ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
                if (applicationRepository.exists(Example.of(probe, matcher))) {
                    // If application exists
                    continue;
                }

                Application application = applicationService.remoteQueryApplication(registryUrl, providerUrl);
                applicationRepository.save(application);
            }
        } else {
            log.info("Discovered offline providers of [{}]", interfaceName);

            // Update providers to inactive
            List<Provider> list = providerRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            list.forEach(provider -> provider.setActive(false));
            providerRepository.saveAll(list);

            // Update application to inactive
            applicationService.inactivate(list.get(0).getApplication(), list.get(0).getRegistryIdentity());
        }
    }
}
