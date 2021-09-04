package org.infinity.luix.demoserver.service.impl;

import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.democommon.service.MailService;

@RpcProvider
public class MailServiceImpl implements MailService {
    @Override
    public String getSenderName() {
        return "louis";
    }
}
