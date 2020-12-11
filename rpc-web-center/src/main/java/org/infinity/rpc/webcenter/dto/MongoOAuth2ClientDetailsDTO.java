package org.infinity.rpc.webcenter.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.infinity.rpc.webcenter.domain.MongoOAuth2ClientDetails;

@ApiModel("单点登录客户端信息DTO")
@Data
public class MongoOAuth2ClientDetailsDTO extends MongoOAuth2ClientDetails {

    private static final long serialVersionUID = 1L;

}
