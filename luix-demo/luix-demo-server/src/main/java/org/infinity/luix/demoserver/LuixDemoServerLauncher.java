package org.infinity.luix.demoserver;

import org.infinity.luix.spring.boot.EnableLuixRpc;
import org.infinity.luix.spring.enhancement.kryo.serializer.*;
import org.infinity.luix.utilities.serializer.kryo.KryoUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.annotation.PostConstruct;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableLuixRpc
public class LuixDemoServerLauncher {
    public static ConfigurableApplicationContext applicationContext;

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        fixM1CpuIssue();
        applicationContext = SpringApplication.run(LuixDemoServerLauncher.class, args);
    }

    private static void fixM1CpuIssue() {
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            // Resolve flapdoodle embed db compatibility issue on M1 CPU
            // Refer: https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/337
            System.setProperty("os.arch", "x86_64");
        }
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
