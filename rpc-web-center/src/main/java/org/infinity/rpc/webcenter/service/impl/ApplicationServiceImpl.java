package org.infinity.rpc.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.domain.Application;
import org.infinity.rpc.webcenter.domain.Consumer;
import org.infinity.rpc.webcenter.domain.Provider;
import org.infinity.rpc.webcenter.repository.ApplicationRepository;
import org.infinity.rpc.webcenter.repository.ConsumerRepository;
import org.infinity.rpc.webcenter.repository.ProviderRepository;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.service.ApplicationService;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.server.buildin.BuildInService.METHOD_GET_APPLICATION_CONFIG;
import static org.infinity.rpc.webcenter.domain.Application.*;
import static org.infinity.rpc.webcenter.domain.Provider.FIELD_REGISTRY_IDENTITY;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    @Resource
    private InfinityProperties    infinityProperties;
    @Resource
    private ApplicationContext    applicationContext;
    @Resource
    private MongoTemplate         mongoTemplate;
    @Resource
    private ProviderRepository    providerRepository;
    @Resource
    private ConsumerRepository    consumerRepository;
    @Resource
    private ApplicationRepository applicationRepository;

    @Override
    public Page<Application> find(Pageable pageable, String registryUrl, String name, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryUrl));
        if (StringUtils.isNotEmpty(name)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_NAME).regex(pattern));
        }
        if (active != null) {
            if (Boolean.TRUE.equals(active)) {
                // or criteria
                Criteria criteria = new Criteria().orOperator(
                        Criteria.where(FIELD_ACTIVE_PROVIDER).is(true),
                        Criteria.where(FIELD_ACTIVE_CONSUMER).is(true));
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where(FIELD_ACTIVE_PROVIDER).is(false));
                query.addCriteria(Criteria.where(FIELD_ACTIVE_CONSUMER).is(false));
            }
        }

        long totalCount = mongoTemplate.count(query, Application.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, Application.class), pageable, totalCount);
    }

    @Override
    public Application remoteQueryApplication(Url registryUrl, Url url) {
        RegistryService registryService = applicationContext.getBean(RegistryService.class);
        RegistryConfig registryConfig = registryService.findRegistryConfig(registryUrl.getIdentity());
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());

        ConsumerStub<?> consumerStub = ConsumerStub.create(BuildInService.class.getName(),
                infinityProperties.getApplication(), registryConfig,
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, url.getAddress(), null, null, 10000, null);
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ApplicationConfig applicationConfig = (ApplicationConfig)
                invocationHandler.invoke(METHOD_GET_APPLICATION_CONFIG, null, null);

        Application application = new Application();
        BeanUtils.copyProperties(applicationConfig, application);
        application.setRegistryIdentity(registryUrl.getIdentity());
        application.setActiveProvider(true);
        application.setActiveConsumer(false);
        return application;
    }

    @Override
    public void inactivate(String applicationName, String registryIdentity) {
        Provider providerProbe = new Provider();
        providerProbe.setApplication(applicationName);
        providerProbe.setRegistryIdentity(registryIdentity);
        providerProbe.setActive(true);

        Consumer consumerProbe = new Consumer();
        consumerProbe.setApplication(applicationName);
        consumerProbe.setRegistryIdentity(registryIdentity);
        consumerProbe.setActive(true);

        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        if (!providerRepository.exists(Example.of(providerProbe, matcher)) &&
                !consumerRepository.exists(Example.of(consumerProbe, matcher))) {
            Optional<Application> application = applicationRepository.findByNameAndRegistryIdentity(applicationName,
                    registryIdentity);
            if (!application.isPresent()) {
                return;
            }
            application.get().setActiveProvider(false);
            applicationRepository.save(application.get());
        }
    }
}
