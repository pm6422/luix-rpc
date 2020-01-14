package org.infinity.rpc.appclient.dto;

import io.swagger.annotations.ApiModel;
import org.infinity.rpc.appclient.domain.MongoOAuth2Approval;

@ApiModel("单点登录授权信息DTO")
public class MongoOAuth2ApprovalDTO extends MongoOAuth2Approval {

    private static final long serialVersionUID = 1L;

}
