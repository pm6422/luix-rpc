package org.infinity.app.common.dto;

import java.io.Serializable;

/**
 * A DTO representing a authority.
 */
public class AuthorityDTO implements Serializable {

    private static final long serialVersionUID = 6131756179263179005L;

    private String name;

    private Boolean systemLevel;

    private Boolean enabled;

    public AuthorityDTO() {
    }

    public AuthorityDTO(String name, Boolean systemLevel, Boolean enabled) {
        super();
        this.name = name;
        this.systemLevel = systemLevel;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getSystemLevel() {
        return systemLevel;
    }

    public void setSystemLevel(Boolean systemLevel) {
        this.systemLevel = systemLevel;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AuthorityDTO [name=" + name + ", systemLevel=" + systemLevel + ", enabled=" + enabled + "]";
    }
}