package org.infinity.rpc.core.protocol;

import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = Scope.SINGLETON)
public interface Protocol {

    <T> Requester<T> createRequester(Class<T> interfaceClass, Url providerUrl);

    //    <T> Exporter<T> createExporter(Provider<T> provider, URL url);

    void destroy();
}
