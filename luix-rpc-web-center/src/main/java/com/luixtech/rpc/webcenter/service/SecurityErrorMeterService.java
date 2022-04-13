package com.luixtech.rpc.webcenter.service;

public interface SecurityErrorMeterService {

    void trackTokenInvalidSignature();

    void trackTokenExpired();

    void trackTokenUnsupported();

    void trackTokenMalformed();

}
