package org.infinity.rpc.webcenter.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.infinity.rpc.webcenter.domain.base.BaseAdminMenu;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AdminMenuTreeDTO extends BaseAdminMenu implements Serializable {

    private static final long                   serialVersionUID = -3123723565571697648L;
    private              List<AdminMenuTreeDTO> children;
}
