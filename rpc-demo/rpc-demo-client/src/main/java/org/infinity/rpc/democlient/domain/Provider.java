package org.infinity.rpc.democlient.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the Provider entity.
 */
@ApiModel("服务提供者")
@Document(collection = "Provider")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider implements Serializable {
    @Id
    protected String id;

    private String interfaceName;

    private String form;

    private String version;

    private String application;

    private String host;

    private String address;

    private String providerUrl;

    private String registryUrl;

    private Boolean active;

    /**
     * Set the current time when inserting.
     */
    @CreatedDate
    protected Instant createdTime;

    /**
     * Set the current time when updating.
     */
    @LastModifiedDate
    protected Instant modifiedTime;
}
