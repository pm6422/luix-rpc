package com.luixtech.luixrpc.webcenter.service;

public interface SecurityErrorMeterService {

    void trackTokenInvalidSignature();

    void trackTokenExpired();

    void trackTokenUnsupported();

    void trackTokenMalformed();

}
