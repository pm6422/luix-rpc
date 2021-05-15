package org.infinity.rpc.demoserver;

import org.infinity.rpc.core.serialization.impl.kryo.KryoUtils;
import org.infinity.rpc.demoserver.utils.serializer.PageImplSerializer;
import org.infinity.rpc.demoserver.utils.serializer.PageRequestSerializer;
import org.infinity.rpc.demoserver.utils.serializer.PageableSerializer;
import org.infinity.rpc.demoserver.utils.serializer.SortSerializer;
import org.infinity.rpc.spring.boot.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.annotation.PostConstruct;

@EnableRpc
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class RpcDemoServerLauncher {
    public static ConfigurableApplicationContext applicationContext;

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        applicationContext = SpringApplication.run(RpcDemoServerLauncher.class, args);
    }

    @PostConstruct
    public void registerSerializers() {
        KryoUtils.registerClass(Sort.class, new SortSerializer());
        KryoUtils.registerClass(PageRequest.class, new PageRequestSerializer());
        KryoUtils.registerClass(Pageable.class, new PageableSerializer());
        KryoUtils.registerClass(PageImpl.class, new PageImplSerializer());
    }
}
