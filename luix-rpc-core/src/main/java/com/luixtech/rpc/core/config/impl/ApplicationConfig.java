package com.luixtech.rpc.core.config.impl;

import com.luixtech.rpc.core.config.Configurable;
import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.rpc.core.utils.DebugModeHolder;
import com.luixtech.rpc.core.utils.JarUtils;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

@Data
@Slf4j
public class ApplicationConfig implements Configurable, Serializable {
    private static final long         serialVersionUID = -7916757272373849145L;
    public static final  String       PREFIX           = "application";
    /**
     * Application ID (Keep unique for all applications under the same registry)
     */
    @NotEmpty
    private              String       id;
    /**
     * Application description
     */
    @Size(max = 20)
    private              String       description;
    /**
     * Team name
     */
    @NotEmpty
    private              String       team;
    /**
     * Owner mail whose suffix must be one of {@link #emailSuffixes}
     */
    @NotEmpty
    private              String       ownerEmail;
    /**
     * Valid mail suffix, generally speaking, it refers to the enterprise mailbox. e.g. @baidu.com, @baidu.cn
     */
    @NotEmpty
    private              List<String> emailSuffixes;
    /**
     * Environment variable, e.g. dev, test or prod
     */
    private              String       env;
    /**
     * LUIX version
     */
    private              String       luixVersion      = JarUtils.VERSION;
    /**
     * Spring boot version
     */
    private              String       springBootVersion;
    /**
     * Spring version
     */
    private              String       springVersion;
    /**
     * JDK vendor
     */
    private              String       jdkVendor;
    /**
     * JDK version
     */
    private              String       jdkVersion;
    /**
     * Enable LUIX endpoint
     */
    private              boolean      enableEndpoint   = true;
    /**
     * Debug mode
     */
    private              boolean      debugMode        = false;

    public void init() {
        checkIntegrity();
        checkValidity();
        // Set debug mode
        DebugModeHolder.setDebugMode(debugMode);
        log.info("Luix application configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {
    }

    @Override
    public void checkValidity() {
        emailSuffixes.stream().filter(suffix -> ownerEmail.endsWith(suffix)).findAny()
                .orElseThrow(() -> new RpcConfigException("Please specify a valid ownerEmail!"));
    }
}