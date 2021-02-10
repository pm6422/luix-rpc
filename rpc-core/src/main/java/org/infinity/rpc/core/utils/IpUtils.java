package org.infinity.rpc.core.utils;

import org.infinity.rpc.utilities.network.NetworkUtils;

public abstract class IpUtils {
    private static final String LOCALHOST1 = "localhost";
    private static final String LOCALHOST2 = "127.0.0.1";

    public static String convertToIntranetHost(String host) {
        if (LOCALHOST1.equals(host) || LOCALHOST2.equals(host)) {
            return NetworkUtils.INTRANET_IP;
        }
        return host;
    }
}
