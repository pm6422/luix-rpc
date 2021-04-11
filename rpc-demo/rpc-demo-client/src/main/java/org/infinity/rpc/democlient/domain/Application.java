package org.infinity.rpc.democlient.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the App entity.
 */
@ApiModel("应用")
@Document(collection = "Application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application implements Serializable {
    private static final long   serialVersionUID      = 1L;
    public static final  String FIELD_NAME = "name";
    public static final  String FIELD_ACTIVE_PROVIDER = "activeProvider";
    public static final  String FIELD_ACTIVE_CONSUMER = "activeConsumer";

    @Id
    protected String  id;
    private   String  name;
    private   String  registryUrl;
    private   Boolean activeProvider = false;
    private   Boolean activeConsumer = false;
    private   Instant createdTime;
    private   Instant modifiedTime;
}
