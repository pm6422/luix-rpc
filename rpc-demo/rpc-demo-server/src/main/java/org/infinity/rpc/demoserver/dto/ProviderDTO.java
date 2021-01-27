package org.infinity.rpc.demoserver.dto;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.core.url.Url;

import java.util.List;

@Data
public class ProviderDTO {
    private String            name;
    private String            app;
    private String            activatedTime;
    private List<AddressInfo> activeProviders;
    private List<AddressInfo> inactiveProviders;

    public static ProviderDTO of(String name, List<AddressInfo> activeProviders, List<AddressInfo> inactiveProviders) {
        ProviderDTO dto = new ProviderDTO();
        dto.setName(name);
        dto.setActiveProviders(activeProviders);
        dto.setInactiveProviders(inactiveProviders);
        // Set app
        if (CollectionUtils.isNotEmpty(activeProviders)) {
            Url url = Url.valueOf(activeProviders.get(0).getContents());
            dto.setApp(url.getParameter(Url.PARAM_APP));
        }
        if (CollectionUtils.isNotEmpty(inactiveProviders)) {
            Url url = Url.valueOf(inactiveProviders.get(0).getContents());
            dto.setApp(url.getParameter(Url.PARAM_APP));
        }
        return dto;
    }
}
