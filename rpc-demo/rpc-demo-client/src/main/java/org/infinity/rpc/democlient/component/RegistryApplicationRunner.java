package org.infinity.rpc.democlient.component;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.democlient.service.RegistryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RegistryApplicationRunner implements ApplicationRunner {
    private final RegistryService registryService;

    public RegistryApplicationRunner(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        registryService.init();
    }
}
