package org.infinity.rpc.core.url;

import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.registry.Registry;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.io.IOUtils.DIR_SEPARATOR_UNIX;
import static org.infinity.rpc.core.constant.ServiceConstants.FORM;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION;

/**
 * Url used to represent a provider or client or registry
 */
@Data
public final class Url implements Serializable {
    private static final long                serialVersionUID   = 2970867582138131181L;
    /**
     * URL Pattern
     * <scheme>://<host>:<port>/<path>?<optionKey>=<optionValue>
     * e.g.
     * infinity://172.25.11.79:26010/org.infinity.rpc.service.AppService?healthChecker=false&requestTimeout=1000
     *
     * <scheme>=infinity|direct
     */
    private static final String              URL_PATTERN        = "{0}://{1}:{2}/{3}?{4}";
    private static final String              PROTOCOL_SEPARATOR = "://";
    /**
     * RPC protocol
     */
    private              String              protocol;
    /**
     * RPC server or client host name
     */
    private              String              host;
    /**
     * RPC server or client port
     */
    private              Integer             port;
    /**
     * RPC interface fully-qualified name
     */
    private              String              path;
    /**
     * Extended options
     */
    private              Map<String, String> options            = new ConcurrentHashMap<>();
    /**
     * Extended options which are number types
     * transient fields will be ignored to generate equals() and hashcode() by lombok
     */
    private transient    Map<String, Number> numOptions         = new ConcurrentHashMap<>();

    // ◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘
    // Constants definitions
    // ◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘◘
    /**
     *
     */
    public static final String PARAM_TYPE                 = "type";
    public static final String PARAM_TYPE_PROVIDER        = "provider";
    public static final String PARAM_TYPE_CONSUMER        = "consumer";
    public static final String PARAM_TYPE_REGISTRY        = "registry";
    public static final String PARAM_TYPE_CLIENT          = "client";
    /**
     * todo: check usage
     */
    public static final String PARAM_HOST                 = "host";
    public static final String PARAM_HOST_DEFAULT_VALUE   = "";
    public static final String PARAM_WEIGHT               = "weights";
    public static final String PARAM_WEIGHT_DEFAULT_VALUE = "";
    public static final String PARAM_ACTIVATED_TIME       = "activatedTime";

    /**
     * Prevent instantiation of it outside the class
     */
    private Url() {
    }

    /**
     * URL Pattern: {protocol}://{host}:{port}/{path}?{options}
     *
     * @param protocol protocol
     * @param host     host
     * @param port     port
     * @param path     RPC interface fully-qualified name
     * @param options  options
     * @return create a url
     */
    public static Url of(String protocol, String host, Integer port, String path, Map<String, String> options) {
        Url url = new Url();
        url.setProtocol(protocol);
        url.setHost(host);
        url.setPort(port);
        url.setPath(path);

        url.addOptions(options);
        url.checkIntegrity();
        url.checkValidity();
        return url;
    }

    public static Url of(String protocol, String host, Integer port, String path) {
        return of(protocol, host, port, path, new ConcurrentHashMap<>(16));
    }

    public static Url providerUrl(String protocol, String host, Integer port, String path) {
        return providerUrl(protocol, host, port, path, null, null);
    }

    public static Url providerUrl(String protocol, String host, Integer port, String path, String form, String version) {
        Map<String, String> options = new ConcurrentHashMap<>(16);
        if (StringUtils.isNotEmpty(form)) {
            options.put(FORM, form);
        }
        if (StringUtils.isNotEmpty(version)) {
            options.put(VERSION, version);
        }
        options.put(Url.PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);
        return of(protocol, host, port, path, options);
    }

    /**
     * The consumer url used to export to registry only for consumers discovery management,
     * but it have nothing to do with the service calling.
     *
     * @param protocol protocol
     * @param host     host
     * @param port     port
     * @param path     RPC interface fully-qualified name
     * @return consumer url
     */
    public static Url consumerUrl(String protocol, String host, Integer port, String path) {
        return consumerUrl(protocol, host, port, path, null, null);
    }

    /**
     * The consumer url used to export to registry only for consumers discovery management,
     * but it have nothing to do with the service calling.
     *
     * @param protocol protocol
     * @param host     host
     * @param port     port
     * @param path     RPC interface fully-qualified name
     * @param form     group
     * @param version  version
     * @return consumer url
     */
    public static Url consumerUrl(String protocol, String host, Integer port, String path, String form, String version) {
        Map<String, String> options = new ConcurrentHashMap<>(16);
        if (StringUtils.isNotEmpty(form)) {
            options.put(FORM, form);
        }
        if (StringUtils.isNotEmpty(version)) {
            options.put(VERSION, version);
        }
        options.put(Url.PARAM_TYPE, Url.PARAM_TYPE_CONSUMER);
        return of(protocol, host, port, path, options);
    }

    /**
     * Create a register url
     *
     * @param protocol registry name
     * @param host     registry host
     * @param port     registry port
     * @return registry url
     */
    public static Url registryUrl(String protocol, String host, Integer port) {
        Map<String, String> options = new ConcurrentHashMap<>(16);
        options.put(Url.PARAM_TYPE, Url.PARAM_TYPE_REGISTRY);
        return of(protocol, host, port, Registry.class.getName(), options);
    }


    /**
     * Composition of host + port
     *
     * @return address
     */
    public String getAddress() {
        if (port <= 0) {
            return host;
        }
        return host + ":" + port;
    }

    private void checkIntegrity() {
        Validate.notEmpty(protocol, "Protocol must NOT be empty!");
        Validate.notEmpty(host, "Host must NOT be empty!");
        Validate.notNull(port, "Port must NOT be null!");
    }

    private void checkValidity() {
    }

    public Url copy() {
        Map<String, String> options = new ConcurrentHashMap<>(16);
        if (this.options != null) {
            options.putAll(this.options);
        }
        return of(protocol, host, port, path, options);
    }

    public void addOption(String name, String value) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
            return;
        }
        options.put(name, value);
    }


    public String getOption(String name, String defaultValue) {
        String value = getOption(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public String getOption(String name) {
        return options.get(name);
    }

    private void addOptions(Map<String, String> options) {
        this.options.putAll(options);
    }

    public int getIntOption(String name, int defaultValue) {
        Number n = getNumOptions().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = options.get(name);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumOptions().put(name, i);
        return i;
    }

    public int getIntOption(String name) {
        Number n = getNumOptions().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = options.get(name);
        int i = Integer.parseInt(value);
        getNumOptions().put(name, i);
        return i;
    }

    public Boolean getBooleanOption(String name, boolean defaultValue) {
        Boolean value = getBooleanOption(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public Boolean getBooleanOption(String name) {
        String value = options.get(name);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 返回identity string,如果两个url的identity相同，则表示相同的一个service或者consumer
     *
     * @return identity
     */
    public String getIdentity() {
        if (PARAM_TYPE_REGISTRY.equals(getOption(PARAM_TYPE))) {
            return protocol + PROTOCOL_SEPARATOR + host + ":" + port;
        }
        return protocol + PROTOCOL_SEPARATOR + host + ":" + port
                + "/" + getForm()
                + "/" + getPath()
                + "/" + getVersion()
                + "/" + getOption(PARAM_TYPE, PARAM_TYPE_PROVIDER);
    }

    public static Url valueOf(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url is null");
        }
        String protocol = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> options = new ConcurrentHashMap<>(16);
        // separator between body and options
        int i = url.indexOf("?");
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("&");

            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        options.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        options.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new RpcConfigurationException("url missing protocol: \"" + url + "\"");
            }
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new RpcConfigurationException("url missing protocol: \"" + url + "\"");
                }
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }

        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) {
            host = url;
        }
        return of(protocol, host, port, path, options);
    }

    public String toFullStr() {
        StringBuilder builder = new StringBuilder();
        builder.append(getUri());
        if (MapUtils.isNotEmpty(options)) {
            builder.append("?");
        }
        Iterator<Map.Entry<String, String>> iterator = options.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String name = entry.getKey();
            String value = entry.getValue();
            builder.append(name).append("=").append(value);
            if (iterator.hasNext()) {
                builder.append("&");
            }
        }
        return builder.toString();
    }

    public String getForm() {
        return StringUtils.defaultString(getOption(FORM));
    }

    public String getVersion() {
        return StringUtils.defaultString(getOption(VERSION));
    }

    @Override
    public String toString() {
        // Use simple string in order to output log simply
        return toSimpleString();
    }

    /**
     * Including protocol, host, port, form
     *
     * @return combination string
     */
    public String toSimpleString() {
        if (StringUtils.isEmpty(getForm()) && StringUtils.isEmpty(getVersion())) {
            return getUri();
        }
        StringBuffer sb = new StringBuffer(getUri());
        boolean hasForm = false;
        if (StringUtils.isNotEmpty(getForm())) {
            sb.append("?form=").append(getForm());
            hasForm = true;
        }
        if (StringUtils.isNotEmpty(getVersion())) {
            if (hasForm) {
                sb.append("&version=").append(getVersion());
            } else {
                sb.append("?version=").append(getVersion());
            }
        }
        return sb.toString();
    }

    public String getUri() {
        if (StringUtils.isEmpty(path)) {
            return protocol + PROTOCOL_SEPARATOR + host + ":" + port;
        }
        return protocol + PROTOCOL_SEPARATOR + host + ":" + port + DIR_SEPARATOR_UNIX + path;
    }

    /**
     * Get method level parameter value
     *
     * @param methodName       method name
     * @param methodParameters method parameter class name list string. e.g, java.util.List,java.lang.Long
     * @param name             parameter name
     * @param defaultValue     parameter default value
     * @return value
     */
    public Integer getMethodParameter(String methodName, String methodParameters, String name, int defaultValue) {
        String key = methodName + "(" + methodParameters + ")." + name;
        Number n = getNumOptions().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(methodName, methodParameters, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumOptions().put(key, i);
        return i;
    }

    public String getMethodParameter(String methodName, String methodParameters, String name) {
        String value = getOption(RpcConstants.METHOD_CONFIG_PREFIX + methodName + "(" + methodParameters + ")." + name);
        if (value == null || value.length() == 0) {
            return getOption(name);
        }
        return value;
    }
}
