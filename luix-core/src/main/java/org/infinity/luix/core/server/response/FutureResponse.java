package org.infinity.luix.core.server.response;

public interface FutureResponse extends Responseable, Future {
    void onSuccess(Responseable response);

    void onFailure(Responseable response);

    long getCreatedTime();

    void setReturnType(Class<?> clazz);
}
