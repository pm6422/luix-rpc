package org.infinity.rpc.democlient.service;

import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.democlient.dto.RegistryDTO;

import java.util.List;

public interface RegistryService {

    List<RegistryDTO> getRegistries();

    Registry findRegistry(String url);
}
