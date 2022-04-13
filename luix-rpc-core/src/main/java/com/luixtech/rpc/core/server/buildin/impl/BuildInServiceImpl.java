package com.luixtech.rpc.core.server.buildin.impl;

import com.luixtech.rpc.core.config.impl.ApplicationConfig;
import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.server.buildin.ServerInfo;
import com.luixtech.rpc.core.server.stub.ProviderStub;
import com.luixtech.rpc.core.server.stub.ProviderStubHolder;

import java.util.Date;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;
import static com.luixtech.rpc.metrics.MetricsUtils.getMemoryStatistic;

public class BuildInServiceImpl implements BuildInService {
    @Override
    public ApplicationConfig getApplicationInfo() {
        String stubBeanName = ProviderStub.buildProviderStubBeanName(BuildInService.class.getName());
        return ProviderStubHolder.getInstance().getMap().get(stubBeanName).getApplicationConfig();
    }

    @Override
    public ServerInfo getServerInfo() {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setOsName(defaultString(System.getProperty("os.name")));
        serverInfo.setOsVersion(defaultString(System.getProperty("os.version")));

        serverInfo.setTimeZone(defaultString(System.getProperty("user.timezone")));
        serverInfo.setSystemTime(ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));

        serverInfo.setJdkVendor(defaultString(System.getProperty("java.vm.vendor")));
        serverInfo.setJdkVersion(defaultString(System.getProperty("java.runtime.version")));

        serverInfo.setCpuCores(Runtime.getRuntime().availableProcessors());
        serverInfo.setMemoryStatistic(getMemoryStatistic());
        return serverInfo;
    }
}
