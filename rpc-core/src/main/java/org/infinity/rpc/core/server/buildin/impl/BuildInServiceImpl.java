package org.infinity.rpc.core.server.buildin.impl;

import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.utils.ApplicationConfigHolder;

import java.util.Date;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;
import static org.infinity.rpc.core.server.response.impl.RpcCheckHealthResponse.CHECK_HEALTH_OK;

public class BuildInServiceImpl implements BuildInService {
    @Override
    public ApplicationConfig getApplicationConfig() {
        return ApplicationConfigHolder.get();
    }

    @Override
    public String getHealth() {
        return CHECK_HEALTH_OK;
    }

    @Override
    public String getSystemTime() {
        return ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date());
    }
}
