package org.infinity.luix.webcenter.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class ResetKeyAndPasswordDTO implements Serializable {

    private static final long serialVersionUID = -6442194590613017034L;

    @NotNull
    private String key;

    @NotNull
    @Size(min = ManagedUserDTO.RAW_PASSWORD_MIN_LENGTH, max = ManagedUserDTO.RAW_PASSWORD_MAX_LENGTH)
    private String newPassword;

}
