package com.luixtech.rpc.democlient;

import com.luixtech.framework.EnableLuixWebFramework;
import com.luixtech.rpc.serializer.kryo.KryoUtils;
import com.luixtech.rpc.spring.boot.starter.EnableLuixRpc;
import com.luixtech.rpc.spring.enhancement.kryo.serializer.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
@EnableLuixRpc
@EnableLuixWebFramework
public class LuixRpcDemoClientApplication {

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LuixRpcDemoClientApplication.class, args);
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
