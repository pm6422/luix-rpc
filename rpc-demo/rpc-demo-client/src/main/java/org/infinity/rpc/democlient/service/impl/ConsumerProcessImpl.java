package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.server.listener.ConsumerProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Consumer;
import org.infinity.rpc.democlient.repository.ApplicationRepository;
import org.infinity.rpc.democlient.repository.ConsumerRepository;
import org.infinity.rpc.democlient.service.ApplicationService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class ConsumerProcessImpl implements ConsumerProcessable {

    @Resource
    private ConsumerRepository    consumerRepository;
    @Resource
    private ApplicationRepository applicationRepository;
    @Resource
    private ApplicationService    applicationService;

    @Override
    public void process(Url registryUrl, String interfaceName, List<Url> consumerUrls) {
        if (CollectionUtils.isNotEmpty(consumerUrls)) {
            log.info("Discovered active consumers {}", consumerUrls);
            for (Url consumerUrl : consumerUrls) {
                Consumer provider = Consumer.of(consumerUrl, registryUrl);
                // Insert or update consumer
                consumerRepository.save(provider);

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

                Application application = applicationService.remoteQueryApplication(registryUrl, consumerUrl);
                applicationRepository.save(application);
            }
        } else {
            log.info("Discovered offline consumers of [{}]", interfaceName);

            // Update consumers to inactive
            List<Consumer> list = consumerRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            list.forEach(provider -> provider.setActive(false));
            consumerRepository.saveAll(list);

            // Update application to inactive
            applicationService.inactivate(list.get(0).getApplication(), list.get(0).getRegistryIdentity());
        }
    }
}
