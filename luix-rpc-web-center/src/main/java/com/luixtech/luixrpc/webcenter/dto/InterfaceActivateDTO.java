package com.luixtech.luixrpc.webcenter.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
public class InterfaceActivateDTO {
    private String registryIdentity;

    @NotEmpty
    private String interfaceName;
}
