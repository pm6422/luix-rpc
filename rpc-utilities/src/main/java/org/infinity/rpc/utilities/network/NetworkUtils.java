package org.infinity.rpc.utilities.network;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

@Slf4j
public abstract class NetworkUtils {
    private static final    Pattern     ADDRESS_PATTERN    = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}:\\d{1,5}$");
    private static final    Pattern     IP_PATTERN         = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    public static final     String      LOCALHOST          = "127.0.0.1";
    public static final     String      ANY_HOST           = "0.0.0.0";
    private static volatile InetAddress LOCAL_ADDRESS      = null;
    public static final     String      INFINITY_IP_PREFIX = "INFINITY_IP_PREFIX";

    /**
     * 内网IP
     */
    public static final String INTRANET_IP = getIntranetIp();
    /**
     * 外网IP
     */
    public static final String INTERNET_IP = getInternetIp();

    public static boolean isValidAddress(String address) {
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    public static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return (name != null && !ANY_HOST.equals(name) && !LOCALHOST.equals(name) && IP_PATTERN.matcher(name).matches());
    }


    /**
     * <pre>
     * 查找策略：首先看是否已经查到ip --> 环境变量中指定的ip --> hostname对应的ip --> --> 轮询网卡
     * </pre>
     * <p>
     * Get the valid IP address based on priorities.
     * Configuration priority: environment variables -> java system properties -> host property in config file ->
     * * /etc/hosts -> default network address -> first available network address
     *
     * @return local ip address
     */
    public static String getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS.getHostAddress();
        }
        InetAddress localAddress = null;
        String ipPrefix = System.getenv(INFINITY_IP_PREFIX);
        if (StringUtils.isNotBlank(ipPrefix)) {
            // 环境变量中如果指定了使用的ip前缀，则使用与该前缀匹配的网卡ip作为本机ip
            localAddress = getLocalAddressByNetworkInterface(ipPrefix);
            log.info("get local address by ip prefix: " + ipPrefix + ", address:" + localAddress);
        }
        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressByHostname();
            log.info("get local address by hostname, address:" + localAddress);
        }
        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressByNetworkInterface(null);
            log.info("get local address from network interface. address:" + localAddress);
        }
        if (isValidAddress(localAddress)) {
            LOCAL_ADDRESS = localAddress;
        }
        if (localAddress != null) {
            return localAddress.getHostAddress();
        }

        return null;
    }

    private static InetAddress getLocalAddressByNetworkInterface(String prefix) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            try {
                                InetAddress address = addresses.nextElement();
                                if (isValidAddress(address)) {
                                    if (StringUtils.isBlank(prefix)) {
                                        return address;
                                    }
                                    if (address.getHostAddress().startsWith(prefix)) {
                                        return address;
                                    }
                                }
                            } catch (Throwable e) {
                                log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                            }
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
        }
        return null;
    }

    private static InetAddress getLocalAddressByHostname() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving local address by hostname:" + e);
        }
        return null;
    }

    /**
     * 获得内网IP
     *
     * @return 内网IP
     */
    private static String getIntranetIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获得外网IP
     * https://codereview.stackexchange.com/questions/65071/test-if-given-ip-is-a-public-one
     *
     * @return 外网IP
     */
    private static String getInternetIp() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            Enumeration<InetAddress> address;
            while (networks.hasMoreElements()) {
                address = networks.nextElement().getInetAddresses();
                while (address.hasMoreElements()) {
                    ip = address.nextElement();
                    if (ip instanceof Inet4Address && !ip.isSiteLocalAddress()
                            && !ip.getHostAddress().equals(INTRANET_IP)) {
                        return ip.getHostAddress();
                    }
                }
            }

            // 如果没有外网IP，就返回内网IP
            return INTRANET_IP;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getHostName(SocketAddress socketAddress) {
        if (socketAddress == null) {
            return null;
        }

        if (socketAddress instanceof InetSocketAddress) {
            InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();
            if (addr != null) {
                return addr.getHostAddress();
            }
        }
        return null;
    }


}