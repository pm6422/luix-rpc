package org.infinity.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A DTO representing a authority.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityDTO implements Serializable {

    private static final long serialVersionUID = 6131756179263179005L;

    private String name;

    private Boolean systemLevel;

    private Boolean enabled;

}