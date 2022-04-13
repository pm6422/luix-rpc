package com.luixtech.rpc.demoserver.service.impl;

import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.democommon.service.MailService;

@RpcProvider
public class MailServiceImpl implements MailService {
    @Override
    public String getSenderName() {
        return "louis";
    }
}
