package org.infinity.rpc.demoserver.service;

import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.demoserver.dto.RegistryDTO;

import java.util.List;

public interface RegistryService {

    List<RegistryDTO> getRegistries();

    Registry findRegistry(String url);

    Object getAllApps();
}
