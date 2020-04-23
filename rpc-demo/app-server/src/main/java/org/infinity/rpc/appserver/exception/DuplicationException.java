package org.infinity.rpc.appserver.exception;

public class DuplicationException extends RuntimeException {

    private static final long serialVersionUID = 4161299998151198599L;

    public DuplicationException(String message) {
        super(message);
    }
}
