package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.repository.ApplicationRepository;
import org.infinity.rpc.democlient.repository.ConsumerRepository;
import org.infinity.rpc.democlient.repository.ProviderRepository;
import org.infinity.rpc.democlient.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

import static org.infinity.rpc.core.config.impl.ProviderConfig.METHOD_APPLICATION_META;
import static org.infinity.rpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;

@Service
@Slf4j
public class ProviderProcessImpl implements ProviderProcessable {

    @Resource
    private InfinityProperties    infinityProperties;
    @Resource
    private ApplicationContext    applicationContext;
    @Resource
    private ProviderRepository    providerRepository;
    @Resource
    private ConsumerRepository    consumerRepository;
    @Resource
    private ApplicationRepository applicationRepository;

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

                Application application = queryApplication(providerUrl, registryUrl);
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
            Provider probe = new Provider();
            probe.setApplication(list.get(0).getApplication());
            probe.setRegistryIdentity(list.get(0).getRegistryIdentity());
            probe.setActive(true);
            // Ignore query parameter if it has a null value
            ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
            if (!providerRepository.exists(Example.of(probe, matcher))) {
                Optional<Application> application = applicationRepository.findByNameAndRegistryIdentity(list.get(0).getApplication(),
                        list.get(0).getRegistryIdentity());
                if (!application.isPresent()) {
                    return;
                }
                application.get().setActiveProvider(false);
                applicationRepository.save(application.get());
            }
        }
    }

    private Application queryApplication(Url providerUrl, Url registryUrl) {
        Application application = new Application();
        RegistryService registryService = applicationContext.getBean(RegistryService.class);
        Url copy = providerUrl.copy();
        int timeout = 100000;
        copy.addOption(REQUEST_TIMEOUT, String.valueOf(timeout));
        ConsumerStub<?> consumerStub = registryService.getConsumerStub(registryUrl.getIdentity(), copy);
        consumerStub.setRequestTimeout(timeout);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ApplicationConfig applicationConfig = (ApplicationConfig) invocationHandler.invoke(METHOD_APPLICATION_META, null, null);
        BeanUtils.copyProperties(applicationConfig, application);
        application.setRegistryIdentity(registryUrl.getIdentity());
        application.setActiveProvider(true);
        application.setActiveConsumer(false);
        return application;
    }
}
