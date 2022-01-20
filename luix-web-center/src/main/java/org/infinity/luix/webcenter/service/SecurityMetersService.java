package org.infinity.luix.webcenter.service;

public interface SecurityMetersService {

    void trackTokenInvalidSignature();

    void trackTokenExpired();

    void trackTokenUnsupported();

    void trackTokenMalformed();

}
