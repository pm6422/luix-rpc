package org.infinity.rpc.webcenter.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.rpc.webcenter.config.ApplicationConstants;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
public class ProfileInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String[] activeProfiles;

    private String ribbonEnv;

    private boolean inProduction = false;

    private boolean swaggerEnabled;

    public ProfileInfoDTO(String[] activeProfiles, boolean swaggerEnabled, String ribbonEnv) {
        this.activeProfiles = activeProfiles;
        this.ribbonEnv = ribbonEnv;

        List<String> springBootProfiles = Arrays.asList(activeProfiles);
        if (springBootProfiles.contains(ApplicationConstants.SPRING_PROFILE_PROD)) {
            this.inProduction = true;
        }
        this.swaggerEnabled = swaggerEnabled;
    }
}