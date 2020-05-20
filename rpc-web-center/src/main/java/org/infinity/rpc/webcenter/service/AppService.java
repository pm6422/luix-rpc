package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.App;

import java.util.Set;

public interface AppService {

    App insert(String name, Boolean enabled, Set<String> authorityNames);

    void update(String name, Boolean enabled, Set<String> authorityNames);

}