package org.infinity.rpc.webcenter.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.infinity.rpc.webcenter.domain.MongoOAuth2RefreshToken;

@ApiModel("刷新令牌信息DTO")
@Data
public class MongoOAuth2RefreshTokenDTO extends MongoOAuth2RefreshToken {

    private static final long serialVersionUID = 1L;

}
