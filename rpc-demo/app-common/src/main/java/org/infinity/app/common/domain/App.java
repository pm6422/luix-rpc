package org.infinity.app.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.app.common.dto.AppDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the App entity.
 */
@Document(collection = "App")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class App implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String name;

    private Boolean enabled;

    public AppDTO toDTO() {
        return new AppDTO(this.getName(), this.getEnabled());
    }

    public static App of(AppDTO dto) {
        App target = new App(dto.getName(), dto.getEnabled());
        return target;
    }
}
