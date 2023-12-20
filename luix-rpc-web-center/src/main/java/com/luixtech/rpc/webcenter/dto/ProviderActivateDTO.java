package com.luixtech.rpc.webcenter.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ProviderActivateDTO {
    private String registryIdentity;

    @NotEmpty
    private String providerUrl;
}
