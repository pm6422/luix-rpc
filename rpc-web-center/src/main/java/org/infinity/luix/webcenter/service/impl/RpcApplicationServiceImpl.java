package org.infinity.luix.webcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubFactory;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.luix.webcenter.domain.RpcApplication;
import org.infinity.luix.webcenter.repository.RpcApplicationRepository;
import org.infinity.luix.webcenter.service.RpcApplicationService;
import org.infinity.luix.webcenter.service.RpcConsumerService;
import org.infinity.luix.webcenter.service.RpcProviderService;
import org.infinity.luix.webcenter.service.RpcRegistryService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.server.buildin.BuildInService.METHOD_GET_APPLICATION_INFO;
import static org.infinity.luix.webcenter.domain.RpcApplication.FIELD_ACTIVE;
import static org.infinity.luix.webcenter.domain.RpcApplication.FIELD_NAME;
import static org.infinity.luix.webcenter.domain.RpcProvider.FIELD_REGISTRY_IDENTITY;
import static org.infinity.luix.webcenter.domain.RpcService.generateMd5Id;

@Service
public class RpcApplicationServiceImpl implements RpcApplicationService {
    @Resource
    private InfinityProperties       infinityProperties;
    @Resource
    private ApplicationContext       applicationContext;
    @Resource
    private MongoTemplate            mongoTemplate;
    @Resource
    private RpcApplicationRepository rpcApplicationRepository;
    @Resource
    private RpcProviderService       rpcProviderService;
    @Resource
    private RpcConsumerService       rpcConsumerService;

    @Deprecated
    @Override
    public boolean exists(String registryIdentity, String name) {
        return rpcApplicationRepository.existsByRegistryIdentityAndName(registryIdentity, name);
    }

    @Override
    public Page<RpcApplication> find(Pageable pageable, String registryIdentity, String name, Boolean active) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(name)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_NAME).regex(pattern));
        }
        if (active != null) {
            query.addCriteria(Criteria.where(FIELD_ACTIVE).is(active));
        }

        long totalCount = mongoTemplate.count(query, RpcApplication.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcApplication.class), pageable, totalCount);
    }

    @Override
    public void inactivate(String registryIdentity, String name) {
        if (!rpcProviderService.existsApplication(registryIdentity, name, true)
                && !rpcConsumerService.existsApplication(registryIdentity, name, true)) {
            Optional<RpcApplication> application = rpcApplicationRepository
                    .findByRegistryIdentityAndName(registryIdentity, name);
            if (!application.isPresent()) {
                return;
            }
            application.get().setActive(false);
            rpcApplicationRepository.save(application.get());
        }
    }

    @Override
    public RpcApplication loadApplication(Url registryUrl, Url url) {
        RpcRegistryService rpcRegistryService = applicationContext.getBean(RpcRegistryService.class);
        RegistryConfig registryConfig = rpcRegistryService.findRegistryConfig(registryUrl.getIdentity());
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());

        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(infinityProperties.getApplication(), registryConfig,
                infinityProperties.getAvailableProtocol(), url.getAddress(), BuildInService.class.getName(),
                10000, 2);
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ApplicationConfig applicationConfig = (ApplicationConfig) invocationHandler.invoke(METHOD_GET_APPLICATION_INFO);

        RpcApplication rpcApplication = new RpcApplication();
        BeanUtils.copyProperties(applicationConfig, rpcApplication);
        String id = generateMd5Id(rpcApplication.getName(), registryUrl.getIdentity());
        rpcApplication.setId(id);
        rpcApplication.setRegistryIdentity(registryUrl.getIdentity());
        rpcApplication.setActive(true);
        rpcApplication.setCreatedTime(Instant.now());
        rpcApplication.setModifiedTime(rpcApplication.getCreatedTime());
        return rpcApplication;
    }
}
