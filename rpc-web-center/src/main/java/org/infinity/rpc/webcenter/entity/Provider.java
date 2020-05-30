package org.infinity.rpc.webcenter.entity;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.registry.zookeeper.utils.AddressInfo;

import java.util.List;

@Data
public class Provider {
    private String            name;
    private String            app;
    private String            activatedTime;
    private List<AddressInfo> activeProviders;
    private List<AddressInfo> inactiveProviders;

    public static Provider of(String name, List<AddressInfo> activeProviders, List<AddressInfo> inactiveProviders) {
        Provider provider = new Provider();
        provider.setName(name);
        provider.setActiveProviders(activeProviders);
        provider.setInactiveProviders(inactiveProviders);
        // Set app
        if (CollectionUtils.isNotEmpty(activeProviders)) {
            Url url = Url.valueOf(activeProviders.get(0).getContents());
            provider.setApp(url.getParameter(Url.PARAM_APP));
        }
        if (CollectionUtils.isNotEmpty(inactiveProviders)) {
            Url url = Url.valueOf(inactiveProviders.get(0).getContents());
            provider.setApp(url.getParameter(Url.PARAM_APP));
        }
        return provider;
    }
}
