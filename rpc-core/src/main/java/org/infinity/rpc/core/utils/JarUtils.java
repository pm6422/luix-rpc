package org.infinity.rpc.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.infinity.rpc.core.config.Configurable;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
public abstract class JarUtils {
    /**
     * Read infinity RPC jar version
     *
     * @return version
     */
    public static String readJarVersion() {
        String version = "";
        try {
            version = IOUtils.resourceToString("version.txt",
                    Charset.defaultCharset(),
                    Configurable.class.getClassLoader());
        } catch (IOException e) {
            log.warn("Failed to read Infinity RPC version file!");
        }
        return version;
    }
}
