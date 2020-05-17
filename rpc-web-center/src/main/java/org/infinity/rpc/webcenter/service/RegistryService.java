package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.registry.zookeeper.utils.AddressInfo;

import java.util.List;
import java.util.Map;

public interface RegistryService {

    List<String> getGroups();

    List<String> getProvidersByGroup(String group);

    List<AddressInfo> getNodes(String group, String provider, String statusNode);

    Map<String, Map<String, List<AddressInfo>>> getAllNodes(String group);
}
