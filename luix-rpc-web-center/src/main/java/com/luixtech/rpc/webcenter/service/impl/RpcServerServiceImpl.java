package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.core.config.impl.RegistryConfig;
import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.server.buildin.ServerInfo;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.spring.boot.starter.config.LuixProperties;
import com.luixtech.rpc.webcenter.domain.RpcServer;
import com.luixtech.rpc.webcenter.repository.RpcServerRepository;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import com.luixtech.rpc.webcenter.service.RpcServerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.luixtech.rpc.core.server.buildin.BuildInService.METHOD_GET_SERVER_INFO;
import static com.luixtech.rpc.webcenter.domain.RpcService.generateMd5Id;

@RpcProvider
@AllArgsConstructor
@Slf4j
public class RpcServerServiceImpl implements RpcServerService {
    private static final int                                             PAGE_SIZE              = 100;
    private static final ConcurrentHashMap<String, Pair<String, String>> DISCOVERED_RPC_SERVERS = new ConcurrentHashMap<>();
    private final        LuixProperties                                  luixProperties;
    private final        ApplicationContext                              applicationContext;
    private final        MongoTemplate                                   mongoTemplate;
    private final        RpcServerRepository                             rpcServerRepository;
    private final        RpcProviderService                              rpcProviderService;
    private final        RpcConsumerService                              rpcConsumerService;

    @Override
    public void loadAll() {
        if (MapUtils.isEmpty(DISCOVERED_RPC_SERVERS)) {
            // Sleep for a while in order to decrease CPU occupation, otherwise the CPU occupation will reach to 100%
            return;
        }
        Iterator<Map.Entry<String, Pair<String, String>>> iterator = DISCOVERED_RPC_SERVERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Pair<String, String>> next = iterator.next();
            RpcServer rpcServer = loadServer(Url.valueOf(next.getValue().getLeft()), Url.valueOf(next.getValue().getRight()));
            String id = generateMd5Id(rpcServer.getAddress(), rpcServer.getRegistryIdentity());
            if (!rpcServerRepository.existsById(id)) {
                rpcServerRepository.insert(rpcServer);
            }
            iterator.remove();
        }
    }

    @Override
    public void updateStatus() {
        long total = rpcServerRepository.count();
        long loopCount = total % PAGE_SIZE == 0 ? total / PAGE_SIZE : total / PAGE_SIZE + 1;
        for (int i = 0; i < loopCount; i++) {
            Pageable pageable = PageRequest.of(i, PAGE_SIZE);
            Page<RpcServer> servers = rpcServerRepository.findAll(pageable);
            if (servers.isEmpty()) {
                return;
            }
            servers.getContent().forEach(domain -> {
                if (rpcProviderService.existsAddress(domain.getRegistryIdentity(), domain.getAddress(), true)) {
                    domain.setProviding(true);
                } else {
                    domain.setProviding(false);
                }
                if (rpcConsumerService.existsAddress(domain.getRegistryIdentity(), domain.getAddress(), true)) {
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
            rpcServerRepository.saveAll(servers);
        }
    }

    @Override
    public boolean exists(String registryIdentity, String address) {
        return rpcServerRepository.existsByRegistryIdentityAndAddress(registryIdentity, address);
    }

    @Override
    public Page<RpcServer> find(Pageable pageable, String registryIdentity, String address) {
        Query query = Query.query(Criteria.where(RpcServer.FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(address)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + address + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(RpcServer.FIELD_ADDRESS).regex(pattern));
        }
        long totalCount = mongoTemplate.count(query, RpcServer.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcServer.class), pageable, totalCount);
    }

    @Override
    public void insert(Url registryUrl, Url url, String address) {
        DISCOVERED_RPC_SERVERS.putIfAbsent(address, Pair.of(registryUrl.toFullStr(), url.toFullStr()));
    }

    @Override
    public void deactivate(String registryIdentity, String address) {
        if (!rpcProviderService.existsService(registryIdentity, address, true)
                && !rpcConsumerService.existsService(registryIdentity, address, true)) {
            RpcServer rpcServer = rpcServerRepository.findByRegistryIdentityAndAddress(registryIdentity, address);
            if (rpcServer == null) {
                return;
            }
            rpcServer.setActive(false);
            rpcServerRepository.save(rpcServer);
        }
    }

    @Override
    public RpcServer loadServer(Url registryUrl, Url url) {
        return loadServer(registryUrl.getIdentity(), url.getAddress());
    }

    @Override
    public RpcServer loadServer(String registryIdentity, String address) {
        RpcRegistryService rpcRegistryService = applicationContext.getBean(RpcRegistryService.class);
        RegistryConfig registryConfig = rpcRegistryService.findRegistryConfig(registryIdentity);
        Proxy proxyFactory = Proxy.getInstance(luixProperties.getConsumer().getProxyFactory());

        ConsumerStub<?> consumerStub = BuildInService.createConsumerStub(luixProperties.getApplication(), registryConfig,
                luixProperties.getAvailableProtocol(), address, 60_000, 2);
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        // Send a remote request to get ApplicationConfig
        ServerInfo serverInfo = (ServerInfo) invocationHandler.invoke(METHOD_GET_SERVER_INFO);

        RpcServer rpcServer = new RpcServer();
        BeanUtils.copyProperties(serverInfo, rpcServer);
        String id = generateMd5Id(address, registryIdentity);
        rpcServer.setId(id);
        rpcServer.setRegistryIdentity(registryIdentity);
        rpcServer.setAddress(address);
        rpcServer.setActive(true);
        return rpcServer;
    }
}
