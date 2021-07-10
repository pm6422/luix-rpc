package org.infinity.rpc.core.server.buildin;

import lombok.Data;

@Data
public class ServerInfo {
    private String osName;
    private String osVersion;
    private String timeZone;
    private String systemTime;
    private String jdkVendor;
    private String jdkVersion;
    private int    cpuCore;
    private String memoryStatistic;
}
