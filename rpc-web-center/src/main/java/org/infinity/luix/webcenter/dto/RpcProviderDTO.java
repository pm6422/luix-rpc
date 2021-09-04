package org.infinity.luix.webcenter.dto;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.core.url.Url;

import java.util.List;

import static org.infinity.rpc.core.constant.ApplicationConstants.APP;

@Data
public class RpcProviderDTO {
    private String            name;
    private String            app;
    private String            activatedTime;
    private List<AddressInfo> activeProviders;
    private List<AddressInfo> inactiveProviders;

    public static RpcProviderDTO of(String name, List<AddressInfo> activeProviders, List<AddressInfo> inactiveProviders) {
        RpcProviderDTO dto = new RpcProviderDTO();
        dto.setName(name);
        dto.setActiveProviders(activeProviders);
        dto.setInactiveProviders(inactiveProviders);
        // Set app
        if (CollectionUtils.isNotEmpty(activeProviders)) {
            Url url = Url.valueOf(activeProviders.get(0).getContents());
            dto.setApp(url.getOption(APP));
        }
        if (CollectionUtils.isNotEmpty(inactiveProviders)) {
            Url url = Url.valueOf(inactiveProviders.get(0).getContents());
            dto.setApp(url.getOption(APP));
        }
        return dto;
    }
}
