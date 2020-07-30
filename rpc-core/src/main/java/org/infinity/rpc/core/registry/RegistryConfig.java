package org.infinity.rpc.core.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.infinity.rpc.core.url.Url;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class RegistryConfig {
    private List<Url>      registryUrls;
    private List<Registry> registries;
}
