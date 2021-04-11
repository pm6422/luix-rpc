package org.infinity.rpc.democlient.service.impl;

import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.service.ConsumerStubService;
import org.infinity.rpc.democlient.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

@Service
public class ConsumerStubServiceImpl implements ConsumerStubService {

    @Resource
    private       InfinityProperties infinityProperties;
    private final RegistryService    registryService;

    public ConsumerStubServiceImpl(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    public ConsumerStub<?> getConsumerStub(String registryIdentity, Url providerUrl) {
        String beanName = ConsumerStub.buildConsumerStubBeanName(providerUrl.getPath(), new HashMap<>(0));
        // 代码移到webcenter后可以打开注释
//        if (ConsumerStubHolder.getInstance().get().containsKey(beanName)) {
//            return ConsumerStubHolder.getInstance().get().get(beanName);
//        }
        ConsumerStub<?> consumerStub = ConsumerStub.create(providerUrl.getPath(), infinityProperties.getApplication(),
                registryService.findRegistryConfig(registryIdentity),
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, providerUrl.getForm(), providerUrl.getVersion(),
                providerUrl.getIntOption(REQUEST_TIMEOUT, REQUEST_TIMEOUT_VAL_DEFAULT),
                providerUrl.getIntOption(MAX_RETRIES, MAX_RETRIES_VAL_DEFAULT));
//        ConsumerStubHolder.getInstance().add(beanName, consumerStub);
        return consumerStub;
    }

}
