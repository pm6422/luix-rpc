package com.luixtech.rpc.democommon.domain;

import com.luixtech.rpc.democommon.domain.base.AbstractAuditableDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the DictItem entity.
 */
@EqualsAndHashCode(callSuper = true)
@Document
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class DictItem extends AbstractAuditableDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(required = true)
    @NotNull
    @Indexed
    private String dictCode;

    @Schema(required = true)
    private String dictName;

    @Schema(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "{EP5901}")
    @Indexed
    private String dictItemCode;

    @Schema(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    private String dictItemName;

    private String remark;

    private Boolean enabled;

}