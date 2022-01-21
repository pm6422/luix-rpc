package org.infinity.luix.registry.consul.utils;

import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.UrlUtils;
import org.infinity.luix.registry.consul.ConsulService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinity.luix.core.constant.RpcConstants.NODE_TYPE_SERVICE;

public class ConsulUtils {

    /**
     * service 最长存活周期（Time To Live），单位秒。 每个service会注册一个ttl类型的check，在最长TTL秒不发送心跳
     * 就会将service变为不可用状态。
     */
    public static int TTL = 30;

    /**
     * motan协议在consul tag中的前缀
     */
    public static final String CONSUL_TAG_MOTAN_PROTOCOL = "protocol_";
    public static final String CONSUL_TAG_MOTAN_URL      = "URL_";
    /**
     * motan rpc 在consul service中的前缀
     */
    public static final String CONSUL_SERVICE_MOTAN_PRE  = "motanrpc_";

    /**
     * 判断两个list中的url是否一致。 如果任意一个list为空，则返回false； 此方法并未做严格互相判等
     *
     * @param urls1
     * @param urls2
     * @return
     */
    public static boolean isSame(List<Url> urls1, List<Url> urls2) {
        if (urls1 == null || urls2 == null) {
            return false;
        }
        if (urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }

    /**
     * 根据服务的url生成consul对应的service
     *
     * @param url
     * @return
     */
    public static ConsulService buildService(Url url) {
        ConsulService service = new ConsulService();
        service.setAddress(url.getHost());
        service.setId(ConsulUtils.convertConsulSerivceId(url));
        service.setName(ConsulUtils.convertGroupToServiceName(url.getForm()));
        service.setPort(url.getPort());
        service.setTtl(TTL);

        List<String> tags = new ArrayList<>();
        tags.add(CONSUL_TAG_MOTAN_PROTOCOL + url.getProtocol());
        tags.add(CONSUL_TAG_MOTAN_URL + UrlUtils.urlEncode(url.toFullStr()));
        service.setTags(tags);

        return service;
    }

    /**
     * 根据service生成motan使用的
     *
     * @param service
     * @return
     */
    public static Url buildUrl(ConsulService service) {
        Url url = null;
        for (String tag : service.getTags()) {
            if (tag.startsWith(CONSUL_TAG_MOTAN_URL)) {
                String encodeUrl = tag.substring(tag.indexOf("_") + 1);
                url = Url.valueOf(UrlUtils.urlDecode(encodeUrl));
            }
        }
        if (url == null) {
            Map<String, String> params = new HashMap<>();
            String group = service.getName().substring(CONSUL_SERVICE_MOTAN_PRE.length());
            params.put(Url.PARAM_FROM, group);
            params.put(Url.PARAM_TYPE, NODE_TYPE_SERVICE);
            String protocol = ConsulUtils.getProtocolFromTag(service.getTags().get(0));
            url = Url.of(protocol, service.getAddress(), service.getPort(),
                    ConsulUtils.getPathFromServiceId(service.getId()), params);
        }
        return url;
    }

    /**
     * 根据url获取cluster信息，cluster 信息包括协议和path（rpc服务中的接口类）。
     *
     * @param url
     * @return
     */
    public static String getUrlClusterInfo(Url url) {
        return url.getProtocol() + "-" + url.getPath();
    }

    /**
     * 有motan的group生成consul的serivce name
     *
     * @param group
     * @return
     */
    public static String convertGroupToServiceName(String group) {
        return CONSUL_SERVICE_MOTAN_PRE + group;
    }

    /**
     * 从consul的service name中获取motan的group
     *
     * @param group
     * @return
     */
    public static String getGroupFromServiceName(String group) {
        return group.substring(CONSUL_SERVICE_MOTAN_PRE.length());
    }

    /**
     * 根据motan的url生成consul的serivce id。 serviceid 包括ip＋port＋rpc服务的接口类名
     *
     * @param url
     * @return
     */
    public static String convertConsulSerivceId(Url url) {
        if (url == null) {
            return null;
        }
        return convertServiceId(url.getHost(), url.getPort(), url.getPath());
    }

    /**
     * 从consul 的serviceid中获取rpc服务的接口类名（url的path）
     *
     * @param serviceId
     * @return
     */
    public static String getPathFromServiceId(String serviceId) {
        return serviceId.substring(serviceId.indexOf("-") + 1);
    }

    /**
     * 从consul的tag获取motan的protocol
     *
     * @param tag
     * @return
     */
    public static String getProtocolFromTag(String tag) {
        return tag.substring(CONSUL_TAG_MOTAN_PROTOCOL.length());
    }


    public static String convertServiceId(String host, int port, String path) {
        return host + ":" + port + "-" + path;
    }

}
