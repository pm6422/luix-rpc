package org.infinity.luix.core.constant;

import java.util.Arrays;
import java.util.List;

import static org.infinity.luix.utilities.serializer.Serializer.*;

public interface ProtocolConstants {

    String       PROTOCOL                           = "protocol";
    String       PROTOCOL_VAL_LUIX                  = "luix";
    String       PROTOCOL_VAL_JVM                   = "jvm";
    String       PROTOCOL_VAL_DEFAULT               = PROTOCOL_VAL_LUIX;
    String       CODEC                              = "codec";
    String       CODEC_VAL_V1                       = "v1";
    String       CODEC_VAL_V2                       = "v2";
    String       CODEC_VAL_DEFAULT                  = CODEC_VAL_V2;
    String       SERIALIZER                         = "serializer";
    String       SERIALIZER_VAL_DEFAULT             = SERIALIZER_NAME_KRYO;
    int          SERIALIZER_ID_DEFAULT              = SERIALIZER_ID_KRYO;
    List<String> SERIALIZERS                        = Arrays.asList(SERIALIZER_NAME_KRYO, SERIALIZER_NAME_HESSIAN2);
    String       NETWORK_TRANSMISSION               = "transmission";
    String       NETWORK_TRANSMISSION_VAL_NETTY     = "netty";
    String       SHARED_SERVER                      = "sharedServer";
    boolean      SHARED_SERVER_VAL_DEFAULT          = true;
    String       MIN_CLIENT_CONN                    = "minClientConn";
    int          MIN_CLIENT_CONN_VAL_DEFAULT        = 2;
    String       MAX_CLIENT_FAILED_CONN             = "maxClientFailedConn";
    int          MAX_CLIENT_FAILED_CONN_VAL_DEFAULT = 10;
    String       MAX_SERVER_CONN                    = "maxServerConn";
    int          MAX_SERVER_CONN_VAL_DEFAULT        = 100000;
    String       MAX_CONTENT_LENGTH                 = "maxContentLength";
    int          MAX_CONTENT_LENGTH_VAL_DEFAULT     = 10 * 1024 * 1024; // 10M
    String       MIN_THREAD                         = "minThread";
    int          MIN_THREAD_VAL_DEFAULT             = 20;
    int          MIN_THREAD_SHARED_CHANNEL          = 40;
    String       MAX_THREAD                         = "maxThread";
    int          MAX_THREAD_VAL_DEFAULT             = 200;
    int          MAX_THREAD_SHARED_CHANNEL          = 800;
    String       WORK_QUEUE_SIZE                    = "workQueueSize";
    int          WORK_QUEUE_SIZE_VAL_DEFAULT        = 0;
    String       ASYNC_CREATE_CONN                  = "asyncCreateConn";
    boolean      ASYNC_CREATE_CONN_VAL_DEFAULT      = false;
    String       THROW_EXCEPTION                    = "throwException";
    boolean      THROW_EXCEPTION_VAL_DEFAULT        = true;
    String       TRANS_EXCEPTION_STACK              = "transExceptionStack";
    boolean      TRANS_EXCEPTION_STACK_VAL_DEFAULT  = true;
}
