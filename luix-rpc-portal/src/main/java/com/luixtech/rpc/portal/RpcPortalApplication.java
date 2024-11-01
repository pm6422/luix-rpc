package com.luixtech.rpc.portal;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.extern.slf4j.Slf4j;

@QuarkusMain
@Slf4j
public class RpcPortalApplication {

    public static void main(String... args) {
        log.info("Running main method");
        Quarkus.run(args);
    }
}