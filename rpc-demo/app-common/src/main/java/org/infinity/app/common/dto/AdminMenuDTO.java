package org.infinity.app.common.dto;

import lombok.Data;
import org.infinity.app.common.entity.MenuTreeNode;
import org.springframework.cglib.beans.BeanCopier;

import java.io.Serializable;

@Data
public class AdminMenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String  id;
    private String  appName;
    private String  name;
    private String  label;
    private Integer level;
    private String  url;
    private Integer sequence;
    private String  parentId;
    private Boolean checked;

    public MenuTreeNode asNode() {
        MenuTreeNode dto = new MenuTreeNode();
        BeanCopier beanCopier = BeanCopier.create(AdminMenuDTO.class, MenuTreeNode.class, false);
        beanCopier.copy(this, dto, null);
        return dto;
    }
}
