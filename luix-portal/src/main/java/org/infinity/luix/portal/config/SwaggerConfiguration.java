package org.infinity.luix.portal.config;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Springfox Swagger configuration.
 * <p>
 * Warning! When having a lot of REST endpoints, Springfox can become a
 * performance issue. In that case, you can use a specific switch {@link ConditionalOnProperty} for
 * this class, so that only front-end developers have access to the Swagger
 * view.
 * <p>
 * https://blog.csdn.net/qq_21948951/article/details/90443723
 */
@Configuration
@EnableSwagger2
@Slf4j
public class SwaggerConfiguration {
    private static final String                GROUP_NAME = "api-group";
    @Resource
    private              ApplicationProperties applicationProperties;


    @Bean
    public Docket apiDocket() {
        log.debug("Building Swagger API docket with group [{}]", GROUP_NAME);
        Docket docket = new Docket(DocumentationType.SWAGGER_2).groupName(GROUP_NAME).apiInfo(apiInfo())
                .forCodeGeneration(true);
        if (System.getProperty("specified.uri.scheme.host") != null
                && "true".equals(System.getProperty("specified.uri.scheme.host"))) {
            docket.host(applicationProperties.getSwagger().getHost());
        }

        docket.genericModelSubstitutes(ResponseEntity.class)
                .ignoredParameterTypes(java.sql.Date.class)
                .directModelSubstitute(java.time.LocalDate.class, java.sql.Date.class)
                .directModelSubstitute(java.time.ZonedDateTime.class, Date.class)
                .directModelSubstitute(java.time.LocalDateTime.class, Date.class)
                .select()
                .apis(RequestHandlerSelectors.basePackage(ApplicationConstants.BASE_PACKAGE))
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .build();
        log.debug("Built Swagger API docket with group [{}]", GROUP_NAME);
        return docket;
    }

    private ApiInfo apiInfo() {
        Contact contact = new Contact(applicationProperties.getSwagger().getContactName(),
                null,
                applicationProperties.getSwagger().getContactEmail());

        return new ApiInfoBuilder()
                .title(applicationProperties.getSwagger().getApi().getTitle())
                .description(applicationProperties.getSwagger().getApi().getDescription())
                .version(applicationProperties.getSwagger().getVersion())
                .contact(contact)
                .build();
    }
}
