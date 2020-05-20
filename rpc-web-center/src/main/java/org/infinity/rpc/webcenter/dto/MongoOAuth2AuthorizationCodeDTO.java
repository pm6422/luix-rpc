package org.infinity.rpc.webcenter.dto;

import io.swagger.annotations.ApiModel;
import org.infinity.rpc.webcenter.domain.MongoOAuth2AuthorizationCode;

@ApiModel("单点登录授权码信息DTO")
public class MongoOAuth2AuthorizationCodeDTO extends MongoOAuth2AuthorizationCode {

    private static final long serialVersionUID = 1L;

}
