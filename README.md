# luix


## Tech points
* 动态代理实现consumer客户端
* 客户端consumer依赖自动扫描发现


## Resolved  issues
##### spring-boot-devtool导致field.getAnnotation(Consumer.class)为null的问题
未引入spring-boot-devtool时field.getAnnotations()[0] instanceof Consumer为true，field.getAnnotations()[0]类型就是Consumer。
但是引入spring-boot-devtool时field.getAnnotations()[0] instanceof Consumer为false。
原因是field.getAnnotations()[0].getClass().getClassLoader()获得的结果为org.springframework.boot.devtools.restart.classloader.RestartClassLoader。
但是Consumer.class.getClassLoader()获得的结果为AppClassLoader。

