package com.luixtech.rpc.webcenter.dto;

import lombok.Data;

import java.util.List;

@Data
public class OptionsDTO {

    private String registryIdentity;

    private String url;

    private List<OptionMetaDTO> options;
}
