package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.webcenter.domain.RpcApplication;
import com.luixtech.rpc.webcenter.domain.RpcService;
import com.luixtech.rpc.webcenter.repository.RpcApplicationRepository;
import com.luixtech.rpc.webcenter.service.RpcApplicationService;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.core.config.impl.ApplicationConfig;
import com.luixtech.rpc.core.config.impl.RegistryConfig;
import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.spring.boot.starter.config.LuixProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.luixtech.rpc.core.server.buildin.BuildInService.METHOD_GET_APPLICATION_INFO;

@RpcProvider
@Slf4j
public class RpcApplicationServiceImpl implements RpcApplicationService {
    private static final ConcurrentHashMap<String, Pair<String, String>> DISCOVERED_RPC_APPLICATIONS = new ConcurrentHashMap<>();
    @Resource
    private              LuixProperties                                  luixProperties;
    @Resource
    private              ApplicationContext                              applicationContext;
    @Resource
    private              MongoTemplate                                   mongoTemplate;
    @Resource
    private              RpcApplicationRepository                        rpcApplicationRepository;
    @Resource
    private              RpcProviderService                              rpcProviderService;
    @Resource
    private              RpcConsumerService                              rpcConsumerService;

    @Override
    public void loadAll() {
        if (MapUtils.isEmpty(DISCOVERED_RPC_APPLICATIONS)) {
            // Sleep for a while in order to decrease CPU occupation, otherwise the CPU occupation will reach to 100%
            return;
        }
        Iterator<Map.Entry<String, Pair<String, String>>> iterator = DISCOVERED_RPC_APPLICATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Pair<String, String>> next = iterator.next();
            RpcApplication rpcApplication = loadApplication(Url.valueOf(next.getValue().getLeft()), Url.valueOf(next.getValue().getRight()));
            String id = RpcService.generateMd5Id(rpcApplication.getId(), rpcApplication.getRegistryIdentity());
            if (!rpcApplicationRepository.existsById(id)) {
                rpcApplicationRepository.insert(rpcApplication);
            }
            iterator.remove();
        }
    }

    @Override
    public void updateStatus() {
        List<RpcApplication> applications = rpcApplicationRepository.findAll();
        if (CollectionUtils.isEmpty(applications)) {
            return;
        }
        applications.forEach(domain -> {
            if (rpcProviderService.existsApplication(domain.getRegistryIdentity(), domain.getId(), true)) {
                domain.setProviding(true);
            } else {
                domain.setProviding(false);
            }
            if (rpcConsumerService.existsApplication(domain.getRegistryIdentity(), domain.getId(), true)) {
                domain.setConsuming(true);
            } else {
                domain.setConsuming(false);
            }
            if (domain.isProviding() || domain.isConsuming()) {
                domain.setActive(true);
            } else {
                domain.setActive(false);
            }
        });
        rpcApplicationRepository.saveAll(applications);
    }

    @Override
    public Page<RpcApplication> find(Pageable pageable, String registryIdentity, String name, Boolean active) {
        Query query = Query.query(Criteria.where(com.luixtech.rpc.webcenter.domain.RpcProvider.FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(name)) {
            // Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(RpcApplication.FIELD_NAME).regex(pattern));
        }
        if (active != null) {
            query.addCriteria(Criteria.where(RpcApplication.FIELD_ACTIVE).is(active));
        }

        long totalCount = mongoTemplate.count(query, RpcApplication.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcApplication.class), pageable, totalCount);
    }

    @Override
    public void insert(Url registryUrl, Url url, String id) {
        DISCOVERED_RPC_APPLICATIONS.putIfAbsent(id, Pair.of(registryUrl.toFullStr(), url.toFullStr()));
    }

    @Override
    public void deactivate(String registryIdentity, String name) {
        if (!rpcProviderService.existsApplication(registryIdentity, name, true)
                && !rpcConsumerService.existsApplication(registryIdentity, name, true)) {
            Optional<RpcApplication> application = rpcApplicationRepository
                    .findByRegistryIdentityAndId(registryIdentity, name);
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
        Proxy proxyFactory = Proxy.getInstance(luixProperties.getConsumer().getProxyFactory());

        ConsumerStub<?> consumerStub = BuildInService.createConsumerStub(luixProperties.getApplication(), registryConfig,
                luixProperties.getAvailableProtocol(), url.getAddress(), 60_000, 2);
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ApplicationConfig applicationConfig = (ApplicationConfig) invocationHandler.invoke(METHOD_GET_APPLICATION_INFO);

        RpcApplication rpcApplication = new RpcApplication();
        BeanUtils.copyProperties(applicationConfig, rpcApplication);
        String id = RpcService.generateMd5Id(rpcApplication.getId(), registryUrl.getIdentity());
        rpcApplication.setTid(id);
        rpcApplication.setRegistryIdentity(registryUrl.getIdentity());
        rpcApplication.setActive(true);
        rpcApplication.setCreatedTime(Instant.now());
        rpcApplication.setModifiedTime(rpcApplication.getCreatedTime());
        return rpcApplication;
    }
}
