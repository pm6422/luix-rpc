package org.infinity.rpc.democlient.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@ApiModel("注册中心")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistryDTO implements Serializable {
    private static final long serialVersionUID = 1617014195662314914L;

    @ApiModelProperty(value = "注册中心类型")
    private String type;

    @ApiModelProperty(value = "URL")
    private String url;
}
