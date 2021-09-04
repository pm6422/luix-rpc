package org.infinity.luix.democommon.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.luix.democommon.domain.base.AbstractAuditableDomain;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the DictItem entity.
 */
@Document(collection = "DictItem")
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class DictItem extends AbstractAuditableDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(required = true)
    @NotNull
    @Indexed
    private String dictCode;

    @ApiModelProperty(required = true)
    private String dictName;

    @ApiModelProperty(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "{EP5901}")
    @Indexed
    private String dictItemCode;

    @ApiModelProperty(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    private String dictItemName;

    private String remark;

    private Boolean enabled;

}