package com.luixtech.rpc.webcenter.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

public abstract class NetworkUtils {
    public static String getRequestUrl(HttpServletRequest request) {
        Objects.requireNonNull(request);

        return request.getScheme() + // "http"
                "://" + // "://"
                request.getServerName() + // "host"
                ":" + // ":"
                request.getServerPort() + // "80"
                request.getContextPath();
    }

    public static String getRequestUrl(HttpServletRequest request, int serverPort) {
        Objects.requireNonNull(request);

        return request.getScheme() + // "http"
                "://" + // "://"
                request.getServerName() + // "host"
                ":" + // ":"
                serverPort + // "80"
                request.getContextPath();
    }
}