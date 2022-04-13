package com.luixtech.rpc.webcenter.domain.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class BaseUser extends AbstractAuditableDomain implements Serializable {

    private static final long   serialVersionUID = -8677592622292657950L;
    public static final  String FIELD_USER_NAME  = "userName";
    public static final  String FIELD_EMAIL      = "email";
    public static final  String FIELD_MOBILE_NO  = "mobileNo";

    @ApiModelProperty(required = true)
    @NotNull
    @Pattern(regexp = "^[a-z0-9-]+$", message = "{EP5901}")
    @Size(min = 3, max = 20)
    @Indexed(unique = true)
    private String userName;

    @ApiModelProperty(required = true)
    @NotNull
    @Size(min = 1, max = 20)
    private String firstName;

    @ApiModelProperty(required = true)
    @NotNull
    @Size(min = 1, max = 20)
    private String lastName;

    @ApiModelProperty(required = true)
    @NotNull
    @Email
    @Size(min = 3, max = 30)
    @Indexed
    private String email;

    @ApiModelProperty(required = true)
    @NotNull
    @Pattern(regexp = "^(13[0-9]|15[012356789]|17[03678]|18[0-9]|14[57])[0-9]{8}$", message = "{EP5951}")
    @Size(min = 11, max = 13)
    @Indexed
    private String mobileNo;

    @JsonIgnore
    private String passwordHash;

    private Boolean activated;

    @JsonIgnore
    private String activationKey;

    @JsonIgnore
    private String resetKey;

    private Instant resetTime;

    private Boolean hasProfilePhoto;

    private Boolean enabled;

    private String remarks;

    @Transient
    private Set<String> authorities;
}
