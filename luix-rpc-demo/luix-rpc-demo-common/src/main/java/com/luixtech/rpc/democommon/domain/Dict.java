package com.luixtech.rpc.democommon.domain;

import com.luixtech.rpc.democommon.domain.base.AbstractAuditableDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the Dict entity.
 */
@EqualsAndHashCode(callSuper = true)
@Document
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class Dict extends AbstractAuditableDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    @Indexed(unique = true)
    private String dictCode;

    @Schema(required = true)
    @NotNull
    @Size(min = 2, max = 50)
    private String dictName;

    private String remark;

    private Boolean enabled;

    public Dict(String dictName, Boolean enabled) {
        this.dictName = dictName;
        this.enabled = enabled;
    }

    public Dict(String dictCode, String dictName, String remark, Boolean enabled) {
        super();
        this.dictCode = dictCode;
        this.dictName = dictName;
        this.remark = remark;
        this.enabled = enabled;
    }
}