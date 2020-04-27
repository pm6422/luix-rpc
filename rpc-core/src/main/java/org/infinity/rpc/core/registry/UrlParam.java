package org.infinity.rpc.core.registry;

import java.util.concurrent.TimeUnit;

public enum UrlParam {
    // registry connect timeout
//    registryConnectTimeout("connectTimeout", TimeUnit.SECONDS.toMillis(1)),
    // registry session timeout
//    registrySessionTimeout("registrySessionTimeout", TimeUnit.MINUTES.toMillis(1)),

    /************************** SPI end ******************************/
//    registryRetryPeriod("registryRetryPeriod", TimeUnit.SECONDS.toMillis(30)),

    group("group", "default_rpc"),
    codec("codec", "motan"),// todo
    check("check", "true"),
    nodeType("nodeType", "service"),
    // 切换group时，各个group的权重比。默认无权重
    weights("weights", "");

    private String  name;
    private String  value;
    private long    longValue;
    private int     intValue;
    private boolean boolValue;

    private UrlParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    private UrlParam(String name, long longValue) {
        this.name = name;
        this.value = String.valueOf(longValue);
        this.longValue = longValue;
    }

    private UrlParam(String name, int intValue) {
        this.name = name;
        this.value = String.valueOf(intValue);
        this.intValue = intValue;
    }

    private UrlParam(String name, boolean boolValue) {
        this.name = name;
        this.value = String.valueOf(boolValue);
        this.boolValue = boolValue;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public boolean getBooleanValue() {
        return boolValue;
    }

}