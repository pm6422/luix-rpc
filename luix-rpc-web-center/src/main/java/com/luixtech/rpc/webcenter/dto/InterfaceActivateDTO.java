package com.luixtech.rpc.webcenter.dto;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
@Builder
public class InterfaceActivateDTO {
    private String registryIdentity;

    @NotEmpty
    private String interfaceName;
}
