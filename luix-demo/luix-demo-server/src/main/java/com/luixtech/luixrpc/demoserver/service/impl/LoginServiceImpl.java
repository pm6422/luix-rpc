package com.luixtech.luixrpc.demoserver.service.impl;

import com.luixtech.luixrpc.core.server.annotation.RpcProvider;
import com.luixtech.luixrpc.democommon.service.LoginService;

@RpcProvider
public class LoginServiceImpl implements LoginService {
    @Override
    public String getUserName() {
        return "louis";
    }
}
