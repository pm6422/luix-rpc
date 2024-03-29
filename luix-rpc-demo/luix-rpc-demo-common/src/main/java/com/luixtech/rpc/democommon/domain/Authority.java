package com.luixtech.rpc.democommon.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the Authority entity.
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Authority implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String DEVELOPER = "ROLE_DEVELOPER";

    public static final String ROLE_SAMPLE_SOUND_USER = "ROLE_SAMPLE_SOUND_USER";

    public static final String SYSTEM_ACCOUNT = "system";

    @Schema(required = true)
    @NotNull
    @Size(min = 3, max = 16)
    @Pattern(regexp = "^[A-Z_]+$", message = "{EP5902}")
    @Id
    private String name;

    private Boolean enabled;

    public Authority(Boolean enabled) {
        this.enabled = enabled;
    }

}
