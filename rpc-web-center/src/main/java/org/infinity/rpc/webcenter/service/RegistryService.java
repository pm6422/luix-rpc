package org.infinity.rpc.webcenter.service;

import java.util.List;
import java.util.Map;

public interface RegistryService {

    List<String> getGroups();

    List<String> getProvidersByGroup(String group);

    List<Map<String, String>> getNodes(String group, String service, String statusNode);

    List<Map<String, List<Map<String, String>>>> getAllNodes(String group);
}
