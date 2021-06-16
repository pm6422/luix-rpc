package org.infinity.rpc.core.constant;

import org.infinity.rpc.core.server.annotation.RpcProvider;

/**
 * All the attribute names of {@link RpcProvider}
 */
public interface ProviderConstants extends ServiceConstants {
    String HEALTH_CHECKER             = "healthChecker";
    String HEALTH_CHECKER_VAL_DEFAULT = "default";
}
