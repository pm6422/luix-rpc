package org.infinity.rpc.appserver.service;

import org.infinity.rpc.appserver.domain.App;

import java.util.Set;

public interface AppService {

    App insert(String name, Boolean enabled, Set<String> authorityNames);

    void update(String name, Boolean enabled, Set<String> authorityNames);

}