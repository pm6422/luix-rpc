package org.infinity.rpc.core.constant;

public interface ProtocolConstants {

    String  PROTOCOL                             = "protocol";
    String  PROTOCOL_DEFAULT_VALUE               = "infinity";
    String  CODEC                                = "codec";
    String  CODEC_DEFAULT_VALUE                  = "default";
    String  SERIALIZER                           = "serializer";
    String  SERIALIZER_DEFAULT_VALUE             = "hessian2";
    String  LOCAL_ADDRESS_FACTORY                = "localAddressFactory";
    String  LOCAL_ADDRESS_FACTORY_DEFAULT_VALUE  = "default";
    String  ENDPOINT_FACTORY                     = "endpointFactory";
    String  ENDPOINT_FACTORY_DEFAULT_VALUE       = "netty";
    String  MIN_CLIENT_CONN                      = "minClientConn";
    int     MIN_CLIENT_CONN_DEFAULT_VALUE        = 2;
    String  MAX_CLIENT_FAILED_CONN               = "maxClientFailedConn";
    int     MAX_CLIENT_FAILED_CONN_DEFAULT_VALUE = 10;
    String  MAX_SERVER_CONN                      = "maxServerConn";
    int     MAX_SERVER_CONN_DEFAULT_VALUE        = 100000;
    String  MAX_CONTENT_LENGTH                   = "maxContentLength";
    int     MAX_CONTENT_LENGTH_DEFAULT_VALUE     = 10 * 1024 * 1024; // 10M
    String  MIN_THREAD                           = "minThread";
    int     MIN_THREAD_DEFAULT_VALUE             = 20;
    int     MIN_THREAD_SHARED_CHANNEL            = 40;
    String  MAX_THREAD                           = "maxThread";
    int     MAX_THREAD_DEFAULT_VALUE             = 200;
    int     MAX_THREAD_SHARED_CHANNEL            = 800;
    String  WORK_QUEUE_SIZE                      = "workQueueSize";
    int     WORK_QUEUE_SIZE_DEFAULT_VALUE        = 0;
    String  SHARE_CHANNEL                        = "shareChannel";
    boolean SHARE_CHANNEL_DEFAULT_VALUE          = true;
}
