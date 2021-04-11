package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.name.ProviderStubBeanNameBuilder;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.repository.ApplicationRepository;
import org.infinity.rpc.democlient.repository.ProviderRepository;
import org.infinity.rpc.democlient.service.ConsumerStubService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

import static org.infinity.rpc.core.constant.ApplicationConstants.APP;
import static org.infinity.rpc.core.server.stub.ProviderStub.APPLICATION_META;

@Service
@Slf4j
public class ProviderProcessServiceImpl implements ProviderProcessable, ApplicationContextAware {

    @Resource
    private       InfinityProperties    infinityProperties;
    private       ApplicationContext    applicationContext;
    private final ProviderRepository    providerRepository;
    private final ApplicationRepository applicationRepository;

    public ProviderProcessServiceImpl(ProviderRepository providerRepository,
                                      ApplicationRepository applicationRepository) {
        this.providerRepository = providerRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
                        .disablePrefix()
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

                // Insert or update provider
                providerRepository.save(provider);

                // Insert application
                if (applicationRepository.countByNameAndRegistryUrl(provider.getApplication(), provider.getRegistryUrl()) > 0) {
                    // Exists
                    return;
                }

                ConsumerStubService consumerStubService = applicationContext.getBean(ConsumerStubService.class);
                ConsumerStub<?> consumerStub = consumerStubService.getConsumerStub(registryUrl.getIdentity(), providerUrl);
                Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
                UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
                ApplicationExtConfig applicationExtConfig = (ApplicationExtConfig) invocationHandler.invoke(APPLICATION_META, null, null);
                Application application = new Application();
                BeanUtils.copyProperties(applicationExtConfig, application);
                application.setRegistryUrl(provider.getRegistryUrl());
                application.setActiveProvider(true);
                applicationRepository.save(application);
            }
        } else {
            log.info("Discovered inactive providers of [{}]", interfaceName);

            // Update providers to inactive
            List<Provider> list = providerRepository.findByInterfaceName(interfaceName);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            list.forEach(provider -> provider.setActive(false));
            providerRepository.saveAll(list);

            // Update application to inactive
            int activeCount = providerRepository.countByApplicationAndRegistryUrlAndActiveIsTrue(list.get(0).getApplication(),
                    list.get(0).getRegistryUrl());
            if (activeCount == 0) {
                Optional<Application> application = applicationRepository.findByNameAndRegistryUrl(list.get(0).getApplication(),
                        list.get(0).getRegistryUrl());
                if (!application.isPresent()) {
                    return;
                }
                application.get().setActiveProvider(false);
                applicationRepository.save(application.get());
            }
        }
    }
}
