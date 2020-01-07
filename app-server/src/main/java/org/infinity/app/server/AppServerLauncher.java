package org.infinity.app.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AppServerLauncher {
    public static void main(String[] args) {
        //启动Spring容器
        new ClassPathXmlApplicationContext("classpath:application.xml");
    }
}
