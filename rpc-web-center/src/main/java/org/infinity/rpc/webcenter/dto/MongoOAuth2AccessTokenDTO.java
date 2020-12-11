package org.infinity.rpc.webcenter.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.infinity.rpc.webcenter.domain.MongoOAuth2AccessToken;

@ApiModel("访问令牌信息DTO")
@Data
public class MongoOAuth2AccessTokenDTO extends MongoOAuth2AccessToken {

    private static final long serialVersionUID = 1L;

}
