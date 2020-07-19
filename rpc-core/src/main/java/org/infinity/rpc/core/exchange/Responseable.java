package org.infinity.rpc.core.exchange;

public interface Responseable<T> extends Exchangable {
    /**
     * Timeout in milliseconds
     * @return
     */
    int getProcessingTimeout();

    /**
     * <pre>
     *  如果正常处理，会返回result，如果处理异常，那么getResult会抛出异常
     * </pre>
     * Response result
     *
     * @return
     */
    Object getResult();

    /**
     * 如果处理异常，那么调用该方法return exception 如果request还没处理完或者request处理正常，那么return null
     * <p>
     * <pre>
     * 		该方法不会阻塞，无论该request是处理中还是处理完成
     * </pre>
     *
     * @return
     */
    Exception getException();
}
