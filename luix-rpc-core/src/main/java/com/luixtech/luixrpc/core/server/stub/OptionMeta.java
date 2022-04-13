package com.luixtech.luixrpc.core.server.stub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionMeta implements Serializable {
    private static final long serialVersionUID = -3886061954129097031L;

    /**
     * Option name
     */
    private String       name;
    /**
     * Option values
     */
    private List<String> values;
    /**
     * Value type
     */
    private String       type;
    /**
     * Default value
     */
    private String       defaultValue;
    /**
     * Updatable
     */
    private boolean      updatable;


}
