package com.luixtech.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.webcenter.domain.RpcService;
import com.luixtech.rpc.webcenter.repository.RpcServiceRepository;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import com.luixtech.rpc.webcenter.service.RpcServiceService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.luixtech.rpc.webcenter.domain.RpcService.*;

@RpcProvider
@Slf4j
public class RpcServiceServiceImpl implements RpcServiceService, ApplicationRunner {

    private static final ConcurrentHashMap<String, RpcService> DISCOVERED_RPC_SERVICES = new ConcurrentHashMap<>();
    private static final int                                   PAGE_SIZE               = 100;
    @Resource
    private              MongoTemplate                         mongoTemplate;
    @Resource
    private              RpcServiceRepository                  rpcServiceRepository;
    @Resource
    private              RpcProviderService                    rpcProviderService;
    @Resource
    private              RpcConsumerService                    rpcConsumerService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(this::execute).start();
    }

    private void execute() {
        while (true) {
            try {
                if (MapUtils.isEmpty(DISCOVERED_RPC_SERVICES)) {
                    // Sleep for a while in order to decrease CPU occupation, otherwise the CPU occupation will reach to 100%
                    TimeUnit.SECONDS.sleep(10L);
                }
                Iterator<Map.Entry<String, RpcService>> iterator = DISCOVERED_RPC_SERVICES.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, RpcService> next = iterator.next();
                    String id = generateMd5Id(next.getValue().getInterfaceName(), next.getValue().getRegistryIdentity());
                    if (!rpcServiceRepository.existsById(id)) {
                        rpcServiceRepository.insert(next.getValue());
                    }
                    iterator.remove();
                }
            } catch (Exception e) {
                log.error("Failed to insert RPC service!", e);
            }
        }
    }

    @Override
    public void updateStatus() {
        long total = rpcServiceRepository.count();
        long loopCount = total % PAGE_SIZE == 0 ? total / PAGE_SIZE : total / PAGE_SIZE + 1;
        for (int i = 0; i < loopCount; i++) {
            Pageable pageable = PageRequest.of(i, PAGE_SIZE);
            Page<RpcService> services = rpcServiceRepository.findAll(pageable);
            if (services.isEmpty()) {
                return;
            }
            services.getContent().forEach(domain -> {
                if (rpcProviderService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
                    domain.setProviding(true);
                    domain.setActive(true);
                }
                if (rpcConsumerService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
                    domain.setConsuming(true);
                    domain.setActive(true);
                }
            });
            rpcServiceRepository.saveAll(services);
        }
    }

    @Override
    public boolean exists(String registryIdentity, String interfaceName) {
        return rpcServiceRepository.existsByRegistryIdentityAndInterfaceName(registryIdentity, interfaceName);
    }

    @Override
    public Page<RpcService> find(Pageable pageable, String registryIdentity, String interfaceName) {
        Query query = Query.query(Criteria.where(FIELD_REGISTRY_IDENTITY).is(registryIdentity));
        if (StringUtils.isNotEmpty(interfaceName)) {
            //Fuzzy search
            Pattern pattern = Pattern.compile("^.*" + interfaceName + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where(FIELD_INTERFACE_NAME).regex(pattern));
        }
        long totalCount = mongoTemplate.count(query, RpcService.class);
        query.with(pageable);
        return new PageImpl<>(mongoTemplate.find(query, RpcService.class), pageable, totalCount);
    }

    @Override
    public void insert(Url registryUrl, String interfaceName) {
        if (DISCOVERED_RPC_SERVICES.containsKey(interfaceName)) {
            return;
        }
        DISCOVERED_RPC_SERVICES.put(interfaceName, RpcService.of(interfaceName, registryUrl));
    }

    @Override
    public void deactivate(String registryIdentity, String interfaceName) {
        if (!rpcProviderService.existsService(registryIdentity, interfaceName, true)
                && !rpcConsumerService.existsService(registryIdentity, interfaceName, true)) {
            RpcService rpcService = rpcServiceRepository
                    .findByRegistryIdentityAndInterfaceName(registryIdentity, interfaceName);
            if (rpcService == null) {
                return;
            }
            rpcService.setActive(false);
            rpcServiceRepository.save(rpcService);
        }
    }
}
