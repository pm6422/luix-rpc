package org.infinity.rpc.core.server.buildin.impl;

import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.buildin.ServerInfo;
import org.infinity.rpc.core.server.stub.MethodData;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.server.stub.ProviderStubHolder;
import org.infinity.rpc.core.utils.ApplicationConfigHolder;

import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;

public class BuildInServiceImpl implements BuildInService {
    @Override
    public ApplicationConfig getApplicationInfo() {
        return ApplicationConfigHolder.get();
    }

    @Override
    public ServerInfo getServerInfo() {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setJdkVendor(defaultString(System.getProperty("java.vm.vendor")));
        serverInfo.setJdkVersion(defaultString(System.getProperty("java.runtime.version")));
        return serverInfo;
    }

    @Override
    public String getSystemTime() {
        return ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date());
    }

    @Override
    public String checkHealth(String interfaceClassName, String form, String version) {
        String providerStubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        return ProviderStubHolder.getInstance().get().get(providerStubBeanName).checkHealth();
    }

    @Override
    public List<MethodData> getMethods(String interfaceClassName, String form, String version) {
        String providerStubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        return ProviderStubHolder.getInstance().get().get(providerStubBeanName).getMethodDataCache();
    }

    @Override
    public void activate(String interfaceClassName, String form, String version) {
        String providerStubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        ProviderStubHolder.getInstance().get().get(providerStubBeanName).activate();
    }

    @Override
    public void deactivate(String interfaceClassName, String form, String version) {
        String providerStubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        ProviderStubHolder.getInstance().get().get(providerStubBeanName).deactivate();
    }
}
