package org.infinity.rpc.utilities.network;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public abstract class AddressUtils {
    public static final  String      LOCALHOST          = "127.0.0.1";
    public static final  String      ANY_HOST           = "0.0.0.0";
    public static final  String      INFINITY_IP_PREFIX = "INFINITY_IP_PREFIX";
    private static final Pattern     ADDRESS_PATTERN    = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}:\\d{1,5}$");
    private static final Pattern     IP_PATTERN         = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static       InetAddress localAddressCache  = null;

    /**
     * Check whether it is the valid IP address
     *
     * @param address IP address
     * @return true: valid, false: invalid
     */
    public static boolean isValidAddress(String address) {
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    /**
     * Check whether it is the valid IP address
     *
     * @param address IP address
     * @return true: valid, false: invalid
     */
    public static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return (name != null && !ANY_HOST.equals(name) && !LOCALHOST.equals(name) && IP_PATTERN.matcher(name).matches());
    }


    /**
     * Get the valid IP address based on priorities.
     * Configuration priority: environment variables > hostname对应的ip -> 轮询网卡
     *
     * @return local ip address
     */
    public static String getLocalAddress() {
        if (localAddressCache != null) {
            return localAddressCache.getHostAddress();
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
            localAddressCache = localAddress;
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

    public static List<Pair<String, Integer>> parseAddress(String address) {
        List<Pair<String, Integer>> results = new ArrayList<>();

        if (address.contains(",")) {
            try {
                String[] addresses = address.split(",");
                for (String addr : addresses) {
                    results.add(parseHostPort(addr));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Illegal format address!");
            }
        } else {
            results.add(parseHostPort(address));
        }
        return results;
    }

    private static Pair<String, Integer> parseHostPort(String addr) {
        String[] hostAndPort = addr.split(":");
        String host = hostAndPort[0].trim();
        int port = Integer.parseInt(hostAndPort[1].trim());
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Illegal port range!");
        }
        return Pair.of(host, port);
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
