package org.infinity.rpc.demoserver.service.impl;


import org.infinity.rpc.demoserver.service.TestService;

public class TestServiceImpl implements TestService {
    @Override
    public String hello(String name) {
        return "hello " + name;
    }
}
