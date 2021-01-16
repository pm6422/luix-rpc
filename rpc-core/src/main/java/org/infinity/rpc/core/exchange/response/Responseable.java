package org.infinity.rpc.core.exchange.response;

import org.infinity.rpc.core.exchange.Exchangable;

public interface Responseable extends Exchangable {
    /**
     * <pre>
     *  如果正常处理，会返回result，如果处理异常，那么getResult会抛出异常
     * </pre>
     * Response result
     *
     * @return response result
     */
    Object getResult();

    /**
     * Timeout in milliseconds
     *
     * @return processing timeout
     */
    int getTimeout();

    /**
     * 如果处理异常，那么调用该方法return exception 如果request还没处理完或者request处理正常，那么return null
     * <p>
     * <pre>
     * 		该方法不会阻塞，无论该request是处理中还是处理完成
     * </pre>
     *
     * @return Exception
     */
    Exception getException();

    /**
     * set the serialization number
     * same to the protocol version, this value only used in server end for compatible.
     *
     * @param number serialization number
     */
    void setSerializeNumber(int number);

    /**
     * Get serialization number
     * @return serialization number
     */
    int getSerializeNumber();
}
