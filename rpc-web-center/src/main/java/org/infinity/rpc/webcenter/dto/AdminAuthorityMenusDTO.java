package org.infinity.rpc.webcenter.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class AdminAuthorityMenusDTO implements Serializable {

    private static final long serialVersionUID = -3119877507448443380L;

    @NotNull
    private String appName;

    @NotNull
    private String authorityName;

    private List<String> adminMenuIds;

}
