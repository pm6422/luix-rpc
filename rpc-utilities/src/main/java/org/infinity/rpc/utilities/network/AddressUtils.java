package org.infinity.rpc.utilities.network;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class AddressUtils {

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
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Illegal port range!");
        }
        return Pair.of(host, port);
    }
}
