package org.infinity.rpc.core.server.buildin.impl;

import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.stub.MethodData;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.server.stub.ProviderStubHolder;
import org.infinity.rpc.core.utils.ApplicationConfigHolder;

import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;

public class BuildInServiceImpl implements BuildInService {
    @Override
    public ApplicationConfig getApplicationConfig() {
        return ApplicationConfigHolder.get();
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
    public void deactivate(String interfaceClassName, String form, String version) {
        String providerStubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        ProviderStubHolder.getInstance().get().get(providerStubBeanName).deactivate();
    }
}
