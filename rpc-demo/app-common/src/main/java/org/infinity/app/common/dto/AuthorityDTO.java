package org.infinity.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * A DTO representing a authority.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityDTO implements Serializable {

    private static final long serialVersionUID = 6131756179263179005L;

    @NotNull
    @Size(min = 3, max = 16)
    @Pattern(regexp = "^[A-Z_]+$", message = "{EP5902}")
    private String name;

    private Boolean systemLevel;

    private Boolean enabled;

}