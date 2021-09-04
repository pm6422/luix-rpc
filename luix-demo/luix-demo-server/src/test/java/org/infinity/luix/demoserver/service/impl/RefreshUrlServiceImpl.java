package org.infinity.luix.demoserver.service.impl;


import org.infinity.luix.demoserver.service.RefreshUrlService;
import org.infinity.luix.demoserver.service.App;

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
