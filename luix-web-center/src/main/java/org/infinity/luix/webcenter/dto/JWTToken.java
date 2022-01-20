package org.infinity.luix.webcenter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

/**
 * Object to return as body in JWT Authentication.
 */
@AllArgsConstructor
public class JWTToken {

    private String idToken;

    @JsonProperty("id_token")
    String getIdToken() {
        return idToken;
    }

    void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}