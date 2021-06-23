package org.infinity.rpc.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.repository.RpcConsumerRepository;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.server.buildin.BuildInService.METHOD_GET_APPLICATION_CONFIG;
import static org.infinity.rpc.webcenter.domain.RpcApplication.*;
import static org.infinity.rpc.webcenter.domain.RpcProvider.FIELD_REGISTRY_IDENTITY;

@Service
public class RpcApplicationServiceImpl implements RpcApplicationService {
    @Resource
    private InfinityProperties    infinityProperties;
    @Resource
    private ApplicationContext    applicationContext;
    @Resource
    private MongoTemplate         mongoTemplate;
    @Resource
    private RpcProviderRepository rpcProviderRepository;
    @Resource
    private RpcConsumerRepository rpcConsumerRepository;
    @Resource
    private RpcApplicationRepository rpcApplicationRepository;

    @Override
    public Page<RpcApplication> find(Pageable pageable, String registryUrl, String name, Boolean active) {
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

        long totalCount = mongoTemplate.count(query, RpcApplication.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcApplication.class), pageable, totalCount);
    }

    @Override
    public RpcApplication remoteQueryApplication(Url registryUrl, Url url) {
        RpcRegistryService rpcRegistryService = applicationContext.getBean(RpcRegistryService.class);
        RegistryConfig registryConfig = rpcRegistryService.findRegistryConfig(registryUrl.getIdentity());
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());

        ConsumerStub<?> consumerStub = ConsumerStub.create(BuildInService.class.getName(),
                infinityProperties.getApplication(), registryConfig,
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, url.getAddress(), null, null, 10000, null);
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ApplicationConfig applicationConfig = (ApplicationConfig)
                invocationHandler.invoke(METHOD_GET_APPLICATION_CONFIG, null, null);

        RpcApplication rpcApplication = new RpcApplication();
        BeanUtils.copyProperties(applicationConfig, rpcApplication);
        rpcApplication.setRegistryIdentity(registryUrl.getIdentity());
        rpcApplication.setActiveProvider(true);
        rpcApplication.setActiveConsumer(false);
        rpcApplication.setCreatedTime(Instant.now());
        rpcApplication.setModifiedTime(rpcApplication.getCreatedTime());
        return rpcApplication;
    }

    @Override
    public void inactivate(String applicationName, String registryIdentity) {
        RpcProvider rpcProviderProbe = new RpcProvider();
        rpcProviderProbe.setApplication(applicationName);
        rpcProviderProbe.setRegistryIdentity(registryIdentity);
        rpcProviderProbe.setActive(true);

        RpcConsumer rpcConsumerProbe = new RpcConsumer();
        rpcConsumerProbe.setApplication(applicationName);
        rpcConsumerProbe.setRegistryIdentity(registryIdentity);
        rpcConsumerProbe.setActive(true);

        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        if (!rpcProviderRepository.exists(Example.of(rpcProviderProbe, matcher)) &&
                !rpcConsumerRepository.exists(Example.of(rpcConsumerProbe, matcher))) {
            Optional<RpcApplication> application = rpcApplicationRepository.findByNameAndRegistryIdentity(applicationName,
                    registryIdentity);
            if (!application.isPresent()) {
                return;
            }
            application.get().setActiveProvider(false);
            rpcApplicationRepository.save(application.get());
        }
    }
}
