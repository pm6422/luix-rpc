package org.infinity.app.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.app.common.dto.AuthorityDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the Authority entity.
 */
@Document(collection = "Authority")
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

    @Id
    private String name;

    private Boolean systemLevel;

    private Boolean enabled;

    public AuthorityDTO toDTO() {
        return new AuthorityDTO(this.getName(), this.getSystemLevel(), this.getEnabled());
    }

    public static Authority of(AuthorityDTO dto) {
        Authority target = new Authority(dto.getName(), dto.getSystemLevel(), dto.getEnabled());
        return target;
    }
}
