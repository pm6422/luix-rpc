package com.luixtech.luixrpc.democlient;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.spring.boot.EnableLuixRpc;
import com.luixtech.luixrpc.spring.enhancement.kryo.serializer.*;
import com.luixtech.luixrpc.utilities.serializer.kryo.KryoUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableLuixRpc
public class LuixDemoClientLauncher {

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LuixDemoClientLauncher.class, args);
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
