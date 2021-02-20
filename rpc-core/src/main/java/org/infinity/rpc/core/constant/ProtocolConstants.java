package org.infinity.rpc.core.constant;

public interface ProtocolConstants {

    String PROTOCOL                            = "protocol";
    String PROTOCOL_DEFAULT_VALUE              = "infinity";
    String CODEC                               = "codec";
    String CODEC_DEFAULT_VALUE                 = "default";
    String SERIALIZER                          = "serializer";
    String SERIALIZER_DEFAULT_VALUE            = "hessian2";
    String LOCAL_ADDRESS_FACTORY               = "localAddressFactory";
    String LOCAL_ADDRESS_FACTORY_DEFAULT_VALUE = "default";
    String  MIN_CLIENT_CONNECTION               = "minClientConnection";
    int     MIN_CLIENT_CONNECTION_DEFAULT_VALUE = 2;
    String  MAX_CLIENT_CONNECTION               = "maxClientConnection";
    int     MAX_CLIENT_CONNECTION_DEFAULT_VALUE = 10;
    String  MAX_SERVER_CONNECTION               = "maxServerConnection";
    int     MAX_SERVER_CONNECTION_DEFAULT_VALUE = 100000;
}
