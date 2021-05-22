package org.infinity.rpc.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class RpcError implements Serializable {

    private static final long   serialVersionUID = -3436571373083796381L;
    /**
     * Error status
     */
    private final        int    status;
    /**
     * Error code
     */
    private final        int    code;
    /**
     * Error message
     */
    private final        String message;
}
