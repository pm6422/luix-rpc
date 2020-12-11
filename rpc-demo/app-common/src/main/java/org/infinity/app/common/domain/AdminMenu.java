package org.infinity.app.common.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.app.common.dto.AdminMenuDTO;
import org.infinity.app.common.entity.MenuTreeNode;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the AdminMenu entity.
 */
@Document(collection = "AdminMenu")
@Data
@NoArgsConstructor
public class AdminMenu implements Serializable {
    private static final long   serialVersionUID = 1L;
    public static final  String FIELD_LEVEL      = "level";
    public static final  String FIELD_SEQUENCE   = "sequence";

    // 主键不要定义为Long型，因为定义为Long型的字段如果超过16位的话在前端页面O会显示为0
    @Id
    private String  id;
    private String  name;
    private String  label;
    @Field(FIELD_LEVEL)
    private Integer level;
    private String  url;
    @Field(FIELD_SEQUENCE)
    private Integer sequence;
    private String  parentId;

    public AdminMenu(String name, String label, Integer level, String url,
                     Integer sequence, String parentId) {
        super();
        this.name = name;
        this.label = label;
        this.level = level;
        this.url = url;
        this.sequence = sequence;
        this.parentId = parentId;
    }

    public AdminMenuDTO toDTO() {
        AdminMenuDTO dto = new AdminMenuDTO();
        BeanCopier beanCopier = BeanCopier.create(AdminMenu.class, AdminMenuDTO.class, false);
        beanCopier.copy(this, dto, null);
        return dto;
    }

    public MenuTreeNode toNode() {
        MenuTreeNode dto = new MenuTreeNode();
        BeanCopier beanCopier = BeanCopier.create(AdminMenu.class, MenuTreeNode.class, false);
        beanCopier.copy(this, dto, null);
        return dto;
    }

    public static AdminMenu of(AdminMenuDTO dto) {
        AdminMenu dest = new AdminMenu();
        BeanCopier beanCopier = BeanCopier.create(AdminMenuDTO.class, AdminMenu.class, false);
        beanCopier.copy(dto, dest, null);
        return dest;
    }
}