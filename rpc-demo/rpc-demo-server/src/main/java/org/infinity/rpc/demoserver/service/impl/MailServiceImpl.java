package org.infinity.rpc.demoserver.service.impl;

import org.infinity.rpc.core.server.annotation.RpcProvider;
import org.infinity.rpc.democommon.service.MailService;

@RpcProvider
public class MailServiceImpl implements MailService {
    @Override
    public String getSenderName() {
        return "louis";
    }
}
