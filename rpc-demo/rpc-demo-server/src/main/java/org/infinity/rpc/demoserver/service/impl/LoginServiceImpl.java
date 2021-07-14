package org.infinity.rpc.demoserver.service.impl;

import org.infinity.rpc.core.server.annotation.RpcProvider;
import org.infinity.rpc.democommon.service.LoginService;

@RpcProvider
public class LoginServiceImpl implements LoginService {
    @Override
    public String getUserName() {
        return "louis";
    }
}
