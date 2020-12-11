package org.infinity.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * A DTO representing an App.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppDTO implements Serializable {

    private static final long serialVersionUID = 6131756179263179005L;

    private String name;

    private Boolean enabled;

    private Set<String> authorities;

    public AppDTO(String name, Boolean enabled) {
        super();
        this.name = name;
        this.enabled = enabled;
    }
}