package org.infinity.rpc.core.config.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.Configurable;
import org.infinity.rpc.core.exception.impl.RpcConfigException;
import org.infinity.rpc.core.utils.DebugModeHolder;
import org.infinity.rpc.core.utils.JarUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Data
@Slf4j
public class ApplicationConfig implements Configurable, Serializable {
    private static final long         serialVersionUID = -7916757272373849145L;
    public static final  String       PREFIX           = "application";
    /**
     * Application name
     * Keep unique
     */
    @NotEmpty
    private              String       name;
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
     * Owner mail whose suffix must be one of {@link #mailSuffixes}
     */
    @NotEmpty
    private              String       ownerMail;
    /**
     * Valid mail suffix, generally speaking, it refers to the enterprise mailbox. e.g. @baidu.com, @baidu.cn
     */
    @NotEmpty
    private              List<String> mailSuffixes;
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
     * Debug mode
     */
    private              boolean      debugMode        = false;

    public void init() {
        checkIntegrity();
        checkValidity();
        // Set debug mode
        DebugModeHolder.setDebugMode(debugMode);
        log.info("Infinity RPC application configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {
    }

    @Override
    public void checkValidity() {
        mailSuffixes.stream().filter(suffix -> ownerMail.endsWith(suffix)).findAny()
                .orElseThrow(() -> new RpcConfigException("Please specify a valid ownerMail!"));
    }
}