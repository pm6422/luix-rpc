package org.infinity.rpc.core.server.stub;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class OptionMeta implements Serializable {
    private static final long serialVersionUID = -3886061954129097031L;

    /**
     * Option name
     */
    private String name;
    /**
     * Option value
     */
    private String value;
    /**
     * Value type
     */
    private String type;
    /**
     * Default value
     */
    private Object defaultValue;
}
