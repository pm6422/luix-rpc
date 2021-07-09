package org.infinity.rpc.core.server.buildin;

import lombok.Data;

@Data
public class ServerInfo {
    private String osVersion;
    private String jdkVendor;
    private String jdkVersion;
    private String cpuCore;
}
