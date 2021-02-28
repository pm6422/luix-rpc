package org.infinity.rpc.democlient.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@ApiModel("泛化调用入参")
@Data
public class GenericCallDTO {
    @ApiModelProperty(value = "接口全路径名称", required = true, example = "org.infinity.rpc.democommon.service.AuthorityService")
    private String interfaceName;

    @ApiModelProperty(value = "方法名称", required = true, example = "findAll")
    private String methodName;

    @ApiModelProperty(value = "方法参数类型全路径名数组", example = "[\"org.infinity.rpc.democommon.domain.Authority\"]", notes = "example里的斜线需要去掉")
    private String[] methodParamTypes;

    @ApiModelProperty(value = "方法参数值", example = "[{\"name\": \"ROLE_TEST\",\"enabled\": true}]", notes = "example里的斜线需要去掉，复杂对象使用map结构")
    private Object[] args;

    @ApiModelProperty(value = "请求选项", required = true, example = "{\"group\": \"default\",\"version\": \"1.0.0\"}", notes = "example里的斜线需要去掉")
    private Map<String, String> options;
}
