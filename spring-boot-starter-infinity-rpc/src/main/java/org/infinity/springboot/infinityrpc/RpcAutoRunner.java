package org.infinity.springboot.infinityrpc;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.server.RpcServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(value = 1)
public class RpcAutoRunner implements ApplicationRunner {

    @Autowired
    private RpcServer rpcServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        rpcServer.startNettyServer();
    }
}
