package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.url.Url;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProviderProcessServiceImpl implements ProviderProcessable {
    @Override
    public void process(Url registryUrl, List<Url> providerUrls, String interfaceName) {
        log.debug("Processing providers");

    }
}
