package com.luixtech.luixrpc.core.server.buildin;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServerInfo implements Serializable {
    private String osName;
    private String osVersion;
    private String timeZone;
    private String systemTime;
    private String jdkVendor;
    private String jdkVersion;
    private int    cpuCores;
    private String memoryStatistic;
}
