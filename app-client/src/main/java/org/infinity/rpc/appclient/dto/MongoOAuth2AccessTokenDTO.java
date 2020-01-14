package org.infinity.rpc.appclient.dto;

import io.swagger.annotations.ApiModel;
import org.infinity.rpc.appclient.domain.MongoOAuth2AccessToken;

@ApiModel("访问令牌信息DTO")
public class MongoOAuth2AccessTokenDTO extends MongoOAuth2AccessToken {

    private static final long serialVersionUID = 1L;

}
