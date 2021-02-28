package org.infinity.rpc.demoserver.service;

import java.util.List;

public interface TestService {
    String hello(String name);

    void save(App app);

    List<App> findAll();
}