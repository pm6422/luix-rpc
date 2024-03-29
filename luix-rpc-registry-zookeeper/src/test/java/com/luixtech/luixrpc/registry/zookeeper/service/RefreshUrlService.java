package com.luixtech.luixrpc.registry.zookeeper.service;

import java.util.List;

public interface RefreshUrlService {
    String hello(String name);

    void save(App app);

    List<App> findAll();
}