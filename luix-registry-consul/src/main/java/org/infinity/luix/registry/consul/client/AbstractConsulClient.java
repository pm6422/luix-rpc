package org.infinity.luix.registry.consul.client;

import org.infinity.luix.registry.consul.ConsulResponse;
import org.infinity.luix.registry.consul.ConsulService;

import java.util.List;

public abstract class AbstractConsulClient {
    protected String host;
    protected int    port;

    public AbstractConsulClient(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    /**
     * 对指定checkid设置为pass状态
     *
     * @param serviceId
     */
    public abstract void checkPass(String serviceId);

    /**
     * 设置checkid为不可用状态。
     *
     * @param serviceId
     */
    public abstract void checkFail(String serviceId);

    /**
     * 注册一个consul service
     *
     * @param service
     */
    public abstract void registerService(ConsulService service);

    /**
     * 根据serviceId注销service
     *
     * @param serviceId
     */
    public abstract void deregisterService(String serviceId);

    /**
     * 获取最新的可用服务列表。
     *
     * @param serviceName
     * @param lastConsulIndex
     * @return
     */
    public abstract ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, long lastConsulIndex);

    /**
     * @param group
     * @return
     */
    public abstract String lookupCommand(String group);
}
