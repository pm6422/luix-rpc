package org.infinity.rpc.core.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class RegistryConfig {
    private List<Url>      registryUrls;
    private List<Registry> registries;
}
