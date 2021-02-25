package org.infinity.rpc.core.provider.service.impl;

import org.infinity.rpc.core.provider.service.TestService;

public class TestServiceImpl implements TestService {
    @Override
    public String hello(String name) {
        return "hello " + name;
    }
}
