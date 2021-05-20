package org.infinity.rpc.core.exception.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class RpcErrorMsg implements Serializable {
    private static final long   serialVersionUID = -3436571373083796381L;
    private final        int    status;
    private final        int    errorCode;
    private final        String message;
}
