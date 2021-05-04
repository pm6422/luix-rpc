package org.infinity.rpc.webcenter.domain.base;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.rpc.webcenter.dto.AdminMenuTreeDTO;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.*;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public abstract class BaseAdminMenu extends AbstractAuditableDomain implements Serializable {

    private static final long   serialVersionUID = -4843853988665463514L;
    public static final  String FIELD_LEVEL      = "level";
    public static final  String FIELD_SEQUENCE   = "sequence";

    @ApiModelProperty(value = "应用名称", required = true)
    @NotNull
    @Size(min = 1, max = 20)
    @Indexed
    private String appName;

    @ApiModelProperty(value = "代码", required = true)
    @NotNull
    @Size(min = 1, max = 30)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "{EP5901}")
    @Indexed(unique = true)
    private String code;

    @ApiModelProperty(value = "名称", required = true)
    @NotNull
    @Size(min = 1, max = 30)
    private String name;

    @ApiModelProperty(value = "层级", required = true)
    @Min(1)
    @Max(9)
    private Integer level;

    @ApiModelProperty(value = "链接地址", required = true)
    @NotNull
    @Size(min = 3, max = 200)
    private String url;

    @ApiModelProperty(value = "排序序号", required = true)
    @Min(1)
    @Max(999)
    private Integer sequence;

    @ApiModelProperty("父菜单ID")
    private String parentId;

    @ApiModelProperty("是否选中")
    @Transient
    private Boolean checked;

    public BaseAdminMenu(String appName, String code, String name, Integer level, String url,
                         Integer sequence, String parentId) {
        super();
        this.appName = appName;
        this.code = code;
        this.name = name;
        this.level = level;
        this.url = url;
        this.sequence = sequence;
        this.parentId = parentId;
    }

    public AdminMenuTreeDTO toTreeDTO() {
        AdminMenuTreeDTO dto = new AdminMenuTreeDTO();
        BeanCopier beanCopier = BeanCopier.create(BaseAdminMenu.class, AdminMenuTreeDTO.class, false);
        beanCopier.copy(this, dto, null);
        return dto;
    }
}