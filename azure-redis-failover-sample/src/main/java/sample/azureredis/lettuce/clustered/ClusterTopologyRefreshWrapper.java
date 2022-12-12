package sample.azureredis.lettuce.clustered;

import io.lettuce.core.*;
import io.lettuce.core.cluster.*;
import io.lettuce.core.cluster.topology.ClusterTopologyRefresh;
import io.lettuce.core.cluster.models.partitions.Partitions;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.Map;

class ClusterTopologyRefreshWrapper implements ClusterTopologyRefresh {
    private final ClusterTopologyRefresh refresh;
    private final Consumer<Throwable> onTopologyRefreshError;

    public ClusterTopologyRefreshWrapper(ClusterTopologyRefresh refresh, Consumer<Throwable> onTopologyRefreshError) {
        this.refresh = refresh;
        this.onTopologyRefreshError = onTopologyRefreshError;
    }

    @Override
    public CompletionStage<Map<RedisURI, Partitions>> loadViews(Iterable<RedisURI> seed, Duration connectTimeout, boolean discovery) {
        return refresh.loadViews(seed, connectTimeout, discovery).whenComplete((ignore, throwable) -> {
            if (throwable != null && onTopologyRefreshError != null) {
                onTopologyRefreshError.accept(throwable);
            }
        });
    }
}