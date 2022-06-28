package com.luixtech.rpc.democommon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AdminMenuDTO implements Serializable {

    private static final long               serialVersionUID = -3123723565571697648L;
    @Schema(required = true)
    private              String             id;
    @Schema(required = true)
    private              String             name;
    @Schema(required = true)
    private              String             url;
    @Schema(required = true)
    private transient    Integer            sequence;
    private              Boolean            checked;
    private              List<AdminMenuDTO> children;
}

