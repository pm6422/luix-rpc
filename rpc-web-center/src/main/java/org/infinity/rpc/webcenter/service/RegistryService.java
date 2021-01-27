package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.AddressInfo;

import java.util.List;
import java.util.Map;

public interface RegistryService {

    List<String> getGroups();

    List<String> getProvidersByGroup(String group);

    List<AddressInfo> getNodes(String group, String provider, String statusNode);

    Map<String, Map<String, List<AddressInfo>>> getAllNodes(String group);

    List<ApplicationExtConfig> getAllApplications();
}
