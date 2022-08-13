package com.luixtech.rpc.webcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A DTO representing a username and password
 */
@Schema(title = "用户名密码DTO")
@Data
@Builder
public class UsernameAndPasswordDTO {
    private String username;

    @Schema(required = true)
    @NotNull
    @Size(min = ManagedUserDTO.RAW_PASSWORD_MIN_LENGTH, max = ManagedUserDTO.RAW_PASSWORD_MAX_LENGTH)
    private String newPassword;
}
