package org.infinity.rpc.spring.boot.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

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
            version = StreamUtils.copyToString(new ClassPathResource("version.txt").getInputStream(),
                    Charset.defaultCharset());
        } catch (IOException e) {
            log.warn("Failed to read Infinity RPC version file!");
        }
        return version;
    }
}
