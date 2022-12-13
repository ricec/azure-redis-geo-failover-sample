package sample.azureredis.lettuce.clustered;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.topology.ClusterTopologyRefresh;
import io.lettuce.core.resource.ClientResources;

import java.util.function.Consumer;
import java.util.Collections;

class ClusterClient extends RedisClusterClient {
    private final Consumer<Throwable> onTopologyRefreshError;

    public ClusterClient(ClientResources clientResources, RedisURI redisURI, Consumer<Throwable> onTopologyRefreshError) {
        super(clientResources, Collections.singleton(redisURI));
        this.onTopologyRefreshError = onTopologyRefreshError;
    }

    @Override
    protected ClusterTopologyRefresh createTopologyRefresh() {
        return new ClusterTopologyRefreshWrapper(super.createTopologyRefresh(), throwable -> onTopologyRefreshError.accept(throwable));
    }
}