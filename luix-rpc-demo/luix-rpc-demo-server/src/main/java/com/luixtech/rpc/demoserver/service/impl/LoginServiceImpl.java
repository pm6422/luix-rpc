package com.luixtech.rpc.demoserver.service.impl;

import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.democommon.service.LoginService;

@RpcProvider
public class LoginServiceImpl implements LoginService {
    @Override
    public String getUserName() {
        return "louis";
    }
}
