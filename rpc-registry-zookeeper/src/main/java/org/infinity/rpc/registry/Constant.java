package org.infinity.rpc.registry;

public interface Constant {
    //定义客户端连接session会话超时时间,单位为毫秒,该值的设置和zkServer设置的心跳时间有关系
    int    SESSION_TIMEOUT = 400000;
    // 定义用于保存rpc通信服务端的地址信息的目录
    String REGISTRY_PATH   = "/rpc";
    // 定义数据存放的具体目录
    String DATA_PATH       = REGISTRY_PATH + "/data";
}
