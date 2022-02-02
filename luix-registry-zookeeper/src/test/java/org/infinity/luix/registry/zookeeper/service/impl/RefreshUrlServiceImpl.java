package org.infinity.luix.registry.zookeeper.service.impl;



import org.infinity.luix.registry.zookeeper.service.App;
import org.infinity.luix.registry.zookeeper.service.RefreshUrlService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefreshUrlServiceImpl implements RefreshUrlService {
    private final List<App> list = new ArrayList<>();

    @Override
    public String hello(String name) {
        return "hello " + name;
    }

    @Override
    public void save(App app) {
        list.add(app);
    }

    @Override
    public List<App> findAll() {
        return Collections.unmodifiableList(list);
    }
}
