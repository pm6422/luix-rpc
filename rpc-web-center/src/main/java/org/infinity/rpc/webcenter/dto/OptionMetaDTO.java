package org.infinity.rpc.webcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.server.stub.OptionMeta;
import org.springframework.beans.BeanUtils;

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

    public void setIntValue(Integer intValue) {
        if (intValue != null) {
            value = intValue.toString();
        }
    }

    public static OptionMetaDTO of(OptionMeta optionMeta) {
        OptionMetaDTO dto = new OptionMetaDTO();
        BeanUtils.copyProperties(optionMeta, dto);
        return dto;
    }
}
