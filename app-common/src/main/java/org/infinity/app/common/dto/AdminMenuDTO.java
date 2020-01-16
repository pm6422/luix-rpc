package org.infinity.app.common.dto;

import org.infinity.app.common.entity.MenuTreeNode;
import org.springframework.cglib.beans.BeanCopier;

import java.io.Serializable;

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
    private boolean checked;

    public AdminMenuDTO() {
    }

    public AdminMenuDTO(String id, String appName, String name, String label, Integer level,
                        String url, Integer sequence, String parentId, boolean checked) {
        super();
        this.id = id;
        this.appName = appName;
        this.name = name;
        this.label = label;
        this.level = level;
        this.url = url;
        this.sequence = sequence;
        this.parentId = parentId;
        this.checked = checked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public MenuTreeNode asNode() {
        MenuTreeNode dto = new MenuTreeNode();
        BeanCopier beanCopier = BeanCopier.create(AdminMenuDTO.class, MenuTreeNode.class, false);
        beanCopier.copy(this, dto, null);
        return dto;
    }

    @Override
    public String toString() {
        return "AdminMenuDTO{" +
                "id='" + id + '\'' +
                ", appName='" + appName + '\'' +
                ", name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", level=" + level +
                ", url='" + url + '\'' +
                ", sequence=" + sequence +
                ", parentId='" + parentId + '\'' +
                ", checked=" + checked +
                '}';
    }
}
