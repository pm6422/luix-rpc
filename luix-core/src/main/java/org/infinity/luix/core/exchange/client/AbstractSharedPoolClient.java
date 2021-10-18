package org.infinity.luix.core.exchange.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.Channel;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.lang.MathUtils;
import org.infinity.luix.utilities.threadpool.NamedThreadFactory;
import org.infinity.luix.utilities.threadpool.NetworkThreadExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
public abstract class AbstractSharedPoolClient extends AbstractClient {

    /**
     * Network thread pool
     */
    private static final ThreadPoolExecutor           NETWORK_THREAD_POOL = new NetworkThreadExecutor(1, 300,
            20000, new NamedThreadFactory(AbstractSharedPoolClient.class.getSimpleName(), true));
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

    public AbstractSharedPoolClient(Url providerUrl) {
        super(providerUrl);
        channelSize = providerUrl.getIntOption(ProtocolConstants.MIN_CLIENT_CONN, ProtocolConstants.MIN_CLIENT_CONN_VAL_DEFAULT);
    }

    protected void initPool() {
        factory = createChannelFactory();
        channels = new ArrayList<>(channelSize);
        IntStream.range(0, channelSize).forEach(x -> channels.add(factory.buildObject()));
        initConnections(providerUrl.getBooleanOption(ProtocolConstants.ASYNC_INIT_CONN, ProtocolConstants.ASYNC_INIT_CONN_VAL_DEFAULT));
    }

    protected void initConnections(boolean async) {
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
                log.error("create connect Error: url=" + providerUrl.getUri(), e);
            }
        }
    }

    protected Channel getChannel() {
        int index = MathUtils.getNonNegativeRange24bit(idx.getAndIncrement());
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

        String errorMsg = this.getClass().getSimpleName() + " getChannel Error: url=" + providerUrl.getUri();
        log.error(errorMsg);
        throw new RpcFrameworkException(errorMsg);
    }

    protected void closeAllChannels() {
        channels.forEach(Channel::close);
    }

    /**
     * Create channel factory
     *
     * @return {@link SharedObjectFactory} instance
     */
    protected abstract SharedObjectFactory<Channel> createChannelFactory();
}
