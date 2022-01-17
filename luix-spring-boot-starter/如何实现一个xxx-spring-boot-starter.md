# 如何实现一个xxx-spring-boot-starter

## 基础篇
### 什么是spring boot starter
starter是spring boot中的一个新发明，它有效的降低了项目开发过程的复杂程度，可以简化开发，提高效率。所以我们可以认为starter其实是把这一些繁琐的配置操作交给了自己，而把简单交给了用户。

### spring boot starter的功能
* 自动帮我们引入相关jar包，完成依赖管理和版本管理
* 自动读取yml、properties等配置文件
* 自动完成相关bean初始化

### spring boot starter的优势
* 通过减少开发人员的配置时间来提高工作效率
* 由于要添加的依赖项数量减少，管理POM更容易
* 无需记住依赖项的名称和版本

### 实现spring boot starter的基本步骤
#### 1. starter命名
spring-boot-starter-xxx是官方提供的starter，xxx-spring-boot-starter是第三方提供的starter。

#### 2. 引入自动配置包及其它相关依赖包
实现starter主要依赖自动配置注解，所以要在pom中引入自动配置相关的jar包。
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
    <version>${spring-boot-autoconfigure_version}</version>
    <scope>provided</scope>
</dependency>
```
除此之外，需要依赖的其他包当然也要引进来。

#### 3. 编写自动配置类
```
@Configuration
@EnableConfigurationProperties({LuixProperties.class})
public class RpcAutoConfiguration {

    @Bean
    public BuildInService buildInService() {
        return new BuildInServiceImpl();
    }
}
```

#### 4. 实现属性配置类
```
@ConfigurationProperties(prefix = PREFIX)
@Data
@Validated
public class LuixProperties implements InitializingBean {

    public static final String                      PREFIX      = "luix";
    @NotNull
    private             ApplicationConfig           application = new ApplicationConfig();
    private             RegistryConfig              registry    = new RegistryConfig();
}
```
#### 5. 实现相关功能类
```
public class BuildInServiceImpl implements BuildInService {
    @Override
    public ApplicationConfig getApplicationInfo() {
    String stubBeanName = ProviderStub.buildProviderStubBeanName(BuildInService.class.getName());
    return ProviderStubHolder.getInstance().getMap().get(stubBeanName).getApplicationConfig();
    }
}
```
## 进阶篇

### 关键类
* @EnableXXX
* @Import
* @Configuration
* @EnableConfigurationProperties
* @ConfigurationProperties
* @ConditionalOnProperty
* @ConditionalOnClass
* @ConditionalOnMissingClass
* @ConditionalOnMissingBean

### 自定义注解service bean的实例化
通过实现BeanDefinitionRegistryPostProcessor接口，通过ClassPathBeanDefinitionRegistryScanner类的scan方法搜索到匹配类、完成实例化并注入spring context。

### 自定义注解service bean的依赖注入
通过实现BeanPostProcessor接口，通过搜索实例所有与需要注入的实例类型一致的字段和方法参数，并完成依赖注入。

### 注意事项
#### maven依赖使用合适的scope
provided scope只在编译时生效，不具有传递性的，也不会被打包。如：
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
    <version>${spring-boot-autoconfigure_version}/version>
    <scope>provided</scope>
</dependency>
```

#### 注解变量使用环境变量
```
@RpcConsumer(providerAddresses = "${application.url.appServiceProviderUrl}")
private AppService         appService;
```

#### 注解变量默认值
Java annotation中字段不能是包装类，会导致布尔型数据只能为true或false，不可以为null。可以使用String类型来解决。

#### 读取properties、yml配置文件
某些场景下需要自己开发PropertySourcesUtils类来读取properties、yml配置文件。

#### 属性配置参数验证
可以通过bean validation实现。

## 代码实践篇
下面会给大家一个具体的代码示例来理解starter。
