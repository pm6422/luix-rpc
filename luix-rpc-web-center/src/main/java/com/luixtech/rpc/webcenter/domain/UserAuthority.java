package com.luixtech.rpc.webcenter.domain;

import com.luixtech.rpc.webcenter.domain.base.AbstractAuditableDomain;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the UserAuthority entity.
 */
@EqualsAndHashCode(callSuper = true)
@Document(collection = "UserAuthority")
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class UserAuthority extends AbstractAuditableDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(required = true)
    @NotNull
    @Indexed
    private String userId;

    @ApiModelProperty(required = true)
    @NotNull
    private String authorityName;

    public UserAuthority(String userId, String authorityName) {
        super();
        this.userId = userId;
        this.authorityName = authorityName;
    }
}
