package org.infinity.luix.webcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.server.stub.OptionMeta;
import org.springframework.beans.BeanUtils;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionMetaDTO extends OptionMeta {
    /**
     * Option value
     */
    private String value;

    public Integer getIntValue() {
        return (StringUtils.isEmpty(value) || "true".equals(value) || "false".equals(value)) ? null : Integer.parseInt(value);
    }

    public Boolean getBooleanValue() {
        return StringUtils.isEmpty(value) ? null : "true".equals(value);
    }

    public static OptionMetaDTO of(OptionMeta optionMeta) {
        OptionMetaDTO dto = new OptionMetaDTO();
        BeanUtils.copyProperties(optionMeta, dto);
        return dto;
    }
}
