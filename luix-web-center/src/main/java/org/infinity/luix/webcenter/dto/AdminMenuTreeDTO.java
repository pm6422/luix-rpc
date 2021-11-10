package org.infinity.luix.webcenter.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AdminMenuTreeDTO implements Serializable {

    private static final long                   serialVersionUID = -3123723565571697648L;
    @ApiModelProperty(required = true)
    private              String                 id;
    @ApiModelProperty(required = true)
    private              String                 name;
    @ApiModelProperty(required = true)
    private              String                 url;
    @ApiModelProperty(required = true)
    private transient    Integer                sequence;
    private              Boolean                checked;
    private              List<AdminMenuTreeDTO> children;
}
