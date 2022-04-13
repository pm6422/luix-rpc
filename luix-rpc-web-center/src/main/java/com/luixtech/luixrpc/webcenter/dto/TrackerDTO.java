package com.luixtech.luixrpc.webcenter.dto;

import lombok.Data;

import java.time.Instant;

/**
 * DTO for storing a user's activity.
 */
@Data
public class TrackerDTO {

    private String sessionId;

    private String userLogin;

    private String ipAddress;

    private String page;

    private Instant time;
}
