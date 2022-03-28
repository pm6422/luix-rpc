package org.infinity.luix.core.exchange.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.Channel;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.lang.MathUtils;
import org.infinity.luix.utilities.threadpool.NamedThreadFactory;
import org.infinity.luix.utilities.threadpool.NetworkThreadPoolExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.infinity.luix.core.constant.ProtocolConstants.*;

@Slf4j
public abstract class AbstractPooledClient extends AbstractClient {

    /**
     * Network thread pool
     */
    private static final ThreadPoolExecutor           NETWORK_THREAD_POOL = new NetworkThreadPoolExecutor(1, 300,
            20000, new NamedThreadFactory(AbstractPooledClient.class.getSimpleName(), true));
    /**
     * Object factory used to build channel
     */
    private              SharedObjectFactory<Channel> factory;
    /**
     * Channel size
     */
    private final        int                          channelSize;
    /**
     * Channels
     */
    private              List<Channel>                channels;
    /**
     * Channel index
     */
    private final        AtomicInteger                idx                 = new AtomicInteger();

    public AbstractPooledClient(Url providerUrl) {
        super(providerUrl);
        channelSize = providerUrl.getIntOption(MIN_CLIENT_CONN, MIN_CLIENT_CONN_VAL_DEFAULT);
    }

    protected void createConnectionPool() {
        factory = createChannelFactory();
        channels = new ArrayList<>(channelSize);
        IntStream.range(0, channelSize).forEach(x -> channels.add(factory.buildObject()));
        boolean asyncCreate = providerUrl.getBooleanOption(ASYNC_CREATE_CONN, ASYNC_CREATE_CONN_VAL_DEFAULT);
        createConnections(asyncCreate);
    }

    protected void createConnections(boolean async) {
        if (async) {
            NETWORK_THREAD_POOL.execute(this::createConnections);
        } else {
            createConnections();
        }
    }

    private void createConnections() {
        for (Channel channel : channels) {
            try {
                channel.open();
            } catch (Exception e) {
                log.error("Failed to create connection for url [" + providerUrl.getUri() + "]", e);
            }
        }
    }

    protected Channel getChannel() {
        int index = MathUtils.getRangedNonNegativeVal(idx.getAndIncrement());
        Channel channel;

        for (int i = index; i < channelSize + 1 + index; i++) {
            channel = channels.get(i % channelSize);
            if (!channel.isActive()) {
                factory.rebuildObject(channel, i != channelSize + 1);
            }
            if (channel.isActive()) {
                return channel;
            }
        }
        String errorMsg = "Failed to get channel for url [" + providerUrl.getUri() + "]";
        throw new RpcFrameworkException(errorMsg);
    }

    protected void closeAllChannels() {
        if (CollectionUtils.isEmpty(channels)) {
            return;
        }
        channels.forEach(Channel::close);
    }

    /**
     * Create channel factory
     *
     * @return {@link SharedObjectFactory} instance
     */
    protected abstract SharedObjectFactory<Channel> createChannelFactory();
}
