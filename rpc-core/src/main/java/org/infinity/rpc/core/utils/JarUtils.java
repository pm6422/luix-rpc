package org.infinity.rpc.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
public abstract class JarUtils {
    public static final String VERSION = getJarVersion();

    /**
     * Read infinity RPC jar version
     *
     * @return version
     */
    public static String getJarVersion() {
        String version = StringUtils.EMPTY;
        try {
            version = IOUtils.resourceToString("version.txt", Charset.defaultCharset(), JarUtils.class.getClassLoader());
        } catch (IOException e) {
            log.warn("Failed to read Infinity RPC version file!");
        }
        return version;
    }
}
