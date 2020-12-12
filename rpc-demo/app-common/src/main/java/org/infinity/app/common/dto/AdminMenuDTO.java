package org.infinity.app.common.dto;

import lombok.Data;
import lombok.ToString;
import org.infinity.app.common.domain.base.AbstractAuditableDomain;

import java.io.Serializable;

@Data
@ToString(callSuper = true)
public class AdminMenuDTO extends AbstractAuditableDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    private String  id;
    private String  appName;
    private String  parentId;
    private String  name;
    private String  label;
    private Integer level;
    private String  url;
    private Integer sequence;
    private Boolean checked;
}
