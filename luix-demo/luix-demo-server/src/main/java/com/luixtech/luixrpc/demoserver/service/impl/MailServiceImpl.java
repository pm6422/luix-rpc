package com.luixtech.luixrpc.demoserver.service.impl;

import com.luixtech.luixrpc.core.server.annotation.RpcProvider;
import com.luixtech.luixrpc.democommon.service.MailService;

@RpcProvider
public class MailServiceImpl implements MailService {
    @Override
    public String getSenderName() {
        return "louis";
    }
}
