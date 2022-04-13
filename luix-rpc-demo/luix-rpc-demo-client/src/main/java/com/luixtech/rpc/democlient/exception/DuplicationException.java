package com.luixtech.rpc.democlient.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DuplicationException extends RuntimeException {

    private static final long                serialVersionUID = 0L;
    private final        Map<String, Object> fieldMap;

    public DuplicationException(Map<String, Object> fieldMap) {
        super("Found duplicated data!");
        this.fieldMap = fieldMap;
    }
}
