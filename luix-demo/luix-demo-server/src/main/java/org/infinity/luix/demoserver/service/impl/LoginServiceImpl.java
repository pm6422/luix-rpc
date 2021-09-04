package org.infinity.luix.demoserver.service.impl;

import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.democommon.service.LoginService;

@RpcProvider
public class LoginServiceImpl implements LoginService {
    @Override
    public String getUserName() {
        return "louis";
    }
}
