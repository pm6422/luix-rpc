package org.infinity.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
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

    @NotNull
    @Size(min = 3, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "{EP5901}")
    private String name;

    private Boolean enabled;

    private Set<String> authorities;

    public AppDTO(String name, Boolean enabled) {
        super();
        this.name = name;
        this.enabled = enabled;
    }
}