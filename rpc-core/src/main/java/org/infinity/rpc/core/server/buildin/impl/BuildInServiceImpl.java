package org.infinity.rpc.core.server.buildin.impl;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.buildin.ServerInfo;
import org.infinity.rpc.core.server.stub.MethodMeta;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.server.stub.ProviderStubHolder;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;
import static org.infinity.rpc.core.constant.ApplicationConstants.APP;
import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.ProviderConstants.HEALTH_CHECKER;
import static org.infinity.rpc.core.constant.ServiceConstants.*;
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

    @Override
    public Map<String, String> getProviderOptions() {
        Map<String, String> options = new HashMap<>();
        options.put(FORM, StringUtils.EMPTY);
        options.put(VERSION, StringUtils.EMPTY);
        options.put(APP, StringUtils.EMPTY);
        options.put(SERIALIZER, StringUtils.EMPTY);
        options.put(HEALTH_CHECKER, StringUtils.EMPTY);
        options.put(REQUEST_TIMEOUT, StringUtils.EMPTY);
        options.put(RETRY_COUNT, StringUtils.EMPTY);
        options.put(MAX_PAYLOAD, StringUtils.EMPTY);
        options.put(CODEC, StringUtils.EMPTY);
        options.put(ENDPOINT_FACTORY, StringUtils.EMPTY);
        options.put(MIN_CLIENT_CONN, StringUtils.EMPTY);
        options.put(MAX_CLIENT_FAILED_CONN, StringUtils.EMPTY);
        options.put(MAX_SERVER_CONN, StringUtils.EMPTY);
        options.put(MAX_CONTENT_LENGTH, StringUtils.EMPTY);
        options.put(MIN_THREAD, StringUtils.EMPTY);
        options.put(MAX_THREAD, StringUtils.EMPTY);
        options.put(WORK_QUEUE_SIZE, StringUtils.EMPTY);
        options.put(SHARED_CHANNEL, StringUtils.EMPTY);
        options.put(ASYNC_INIT_CONN, StringUtils.EMPTY);

        return options;
    }

    @Override
    public Map<String, String> getConsumerOptions() {
        Map<String, String> options = new HashMap<>();
        options.put(FORM, StringUtils.EMPTY);
        options.put(VERSION, StringUtils.EMPTY);
        options.put(APP, StringUtils.EMPTY);
        options.put(SERIALIZER, StringUtils.EMPTY);
        options.put(REQUEST_TIMEOUT, StringUtils.EMPTY);
        options.put(RETRY_COUNT, StringUtils.EMPTY);
        options.put(MAX_PAYLOAD, StringUtils.EMPTY);
        options.put(THROW_EXCEPTION, StringUtils.EMPTY);

        return options;
    }

    @Override
    public String checkHealth(String interfaceClassName, String form, String version) {
        String stubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        return ProviderStubHolder.getInstance().get().get(stubBeanName).checkHealth();
    }

    @Override
    public List<MethodMeta> getMethods(String interfaceClassName, String form, String version) {
        String stubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        return ProviderStubHolder.getInstance().get().get(stubBeanName).getMethodMetaCache();
    }

    @Override
    public void activate(String interfaceClassName, String form, String version) {
        String stubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        ProviderStubHolder.getInstance().get().get(stubBeanName).activate();
    }

    @Override
    public void deactivate(String interfaceClassName, String form, String version) {
        String stubBeanName = ProviderStub.buildProviderStubBeanName(interfaceClassName, form, version);
        ProviderStubHolder.getInstance().get().get(stubBeanName).deactivate();
    }
}
