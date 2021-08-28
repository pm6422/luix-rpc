package org.infinity.rpc.demoserver;

import org.infinity.rpc.spring.boot.EnableRpc;
import org.infinity.rpc.spring.enhancement.kryo.serializer.*;
import org.infinity.rpc.utilities.serializer.kryo.KryoUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.annotation.PostConstruct;

@EnableRpc
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class LuixDemoServerLauncher {
    public static ConfigurableApplicationContext applicationContext;

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        applicationContext = SpringApplication.run(LuixDemoServerLauncher.class, args);
    }

    @PostConstruct
    public void registerSerializers() {
        KryoUtils.registerClass(Sort.class, new SortSerializer());
        KryoUtils.registerClass(PageRequest.class, new PageRequestSerializer());
        KryoUtils.registerClass(Pageable.class, new PageableSerializer());
        KryoUtils.registerClass(PageImpl.class, new PageImplSerializer());
        KryoUtils.registerClass(Page.class, new PageSerializer());
        KryoUtils.registerClass(Criteria.class, new CriteriaSerializer());
    }
}
