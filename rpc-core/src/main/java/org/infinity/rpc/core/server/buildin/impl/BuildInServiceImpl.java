package org.infinity.rpc.core.server.buildin.impl;

import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.buildin.ServerInfo;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.server.stub.ProviderStubHolder;

import java.util.Date;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;
import static org.infinity.rpc.utilities.statistics.StatisticsUtils.getMemoryStatistic;

public class BuildInServiceImpl implements BuildInService {
    @Override
    public ApplicationConfig getApplicationInfo() {
        String stubBeanName = ProviderStub.buildProviderStubBeanName(BuildInService.class.getName());
        return ProviderStubHolder.getInstance().get().get(stubBeanName).getApplicationConfig();
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

        serverInfo.setCpuCore(Runtime.getRuntime().availableProcessors());
        serverInfo.setMemoryStatistic(getMemoryStatistic());
        return serverInfo;
    }
}
