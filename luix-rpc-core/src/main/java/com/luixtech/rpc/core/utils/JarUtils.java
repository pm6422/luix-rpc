package com.luixtech.rpc.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
public abstract class JarUtils {
    public static final String VERSION = getJarVersion();

    /**
     * Read luix jar version
     *
     * @return version
     */
    public static String getJarVersion() {
        try {
            return IOUtils.resourceToString("version.txt", Charset.defaultCharset(), JarUtils.class.getClassLoader());
        } catch (IOException e) {
            log.warn("Failed to read luix RPC jar version!");
            return StringUtils.EMPTY;
        }
    }
}
