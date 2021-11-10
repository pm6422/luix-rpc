package org.infinity.luix.democlient.config;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.problem.ProblemModule;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

@Configuration
public class JacksonConfiguration {

    /**
     * Support for Java date and time API.
     *
     * @return the corresponding Jackson module.
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    public Jdk8Module jdk8TimeModule() {
        return new Jdk8Module();
    }

    /**
     * Jackson Afterburner module to speed up serialization/deserialization.
     *
     * @return module instance
     */
    @Bean
    public AfterburnerModule afterburnerModule() {
        return new AfterburnerModule();
    }

    /**
     * Module for serialization/deserialization of RFC7807 Problem.
     *
     * @return module instance
     */
    @Bean
    public ProblemModule problemModule() {
        return new ProblemModule();
    }

    /**
     * Module for serialization/deserialization of ConstraintViolationProblem.
     *
     * @return module instance
     */
    @Bean
    public ConstraintViolationProblemModule constraintViolationProblemModule() {
        return new ConstraintViolationProblemModule();
    }
}
