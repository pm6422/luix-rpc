package org.infinity.rpc.appclient.service;

import org.infinity.rpc.appclient.domain.App;

import java.util.Set;

public interface AppService {

    App insert(String name, Boolean enabled, Set<String> authorityNames);

    void update(String name, Boolean enabled, Set<String> authorityNames);

}