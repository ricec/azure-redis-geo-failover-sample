package sample.azureredis.lettuce.clustered;

import io.lettuce.core.*;
import io.lettuce.core.api.*;
import io.lettuce.core.api.sync.*;
import io.lettuce.core.cluster.*;
import io.lettuce.core.cluster.api.*;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.function.Function;
import sample.azureredis.lettuce.shared.ErrorHelper;
import sample.azureredis.lettuce.shared.MultiPasswordCredentials;

class ClusterConnectionProvider
{
    private boolean initialized = false;
    private RedisClusterClient redisClient;
    private StatefulRedisClusterConnection<String, String> connection;
    private MultiPasswordCredentials credentials;
    private RedisURI redisURI;

    public ClusterConnectionProvider(String hostname, int port, String password, String secondaryPassword) {
        this.credentials = new MultiPasswordCredentials(password, secondaryPassword);
        this.redisURI = RedisURI.Builder.redis(hostname)
            .withSsl(true)
            .withAuthentication(RedisCredentialsProvider.from(() -> credentials))
            .withClientName("LettuceClient")
            .withPort(port)
            .build();
    }

    public StatefulRedisClusterConnection<String, String> getConnection() {
        if (!initialized) {
            init();
        }

        return connection;
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        }
        finally {
            connection = null;
        }
        try {
            if (redisClient != null) {
                redisClient.shutdown();
            }
        }
        finally {
            redisClient = null;
        }
    }

    private synchronized void init() {
        if (!initialized) {
            redisClient = new ClusterClient(createClientResources(), redisURI, t -> {
                // Only handle auth failures from topology refresh after the connection has been successfully created.
                // Any auth errors during connection creation will be bubbled up, so they can be caught and handleded directly
                // where redisClient.connect() was invoked.
                if (initialized && ErrorHelper.causedByAuthFailure(t)) {
                    handleAuthFailure();
                }
            });
            
            setClusterClientOptions(redisClient);
            
            try {
                connection = redisClient.connect();
            }
            catch (RedisConnectionException e) {
                if (ErrorHelper.causedByAuthFailure(e)) {
                    handleAuthFailure();
                    connection = redisClient.connect();
                }
                else {
                    throw e;
                }
            }

            initialized = true;
        }
    }

    private void handleAuthFailure() {
        System.out.println("Auth failure. Switching to alternate password.");
        credentials.swapPassword();
    }

    // Sets ClusterClientOptions in accordance with the best-practices doc here:
    // https://github.com/Azure/AzureCacheForRedis/blob/main/Lettuce%20Best%20Practices.md
    private void setClusterClientOptions(RedisClusterClient redisClient) {
        redisClient.setOptions(
            ClusterClientOptions.builder()
                .socketOptions(SocketOptions.builder().keepAlive(true).build())
                .topologyRefreshOptions(createTopologyRefreshOptions())
                .build());
    }

    // Provides ClusterTopologyRefreshOptions in accordance with the best-practices doc here:
    // https://github.com/Azure/AzureCacheForRedis/blob/main/Lettuce%20Best%20Practices.md
    private ClusterTopologyRefreshOptions createTopologyRefreshOptions() {
        return ClusterTopologyRefreshOptions.builder()
            .enablePeriodicRefresh(Duration.ofSeconds(5))
            .dynamicRefreshSources(false)
            .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(5))
            .enableAllAdaptiveRefreshTriggers().build();
    }

    // Provides ClientResources in accordance with the best-practices doc here:
    // https://github.com/Azure/AzureCacheForRedis/blob/main/Lettuce%20Best%20Practices.md
    private ClientResources createClientResources() {
        Function<HostAndPort, HostAndPort> mappingFunction = new Function<HostAndPort, HostAndPort>() {
            @Override
            public HostAndPort apply(HostAndPort hostAndPort) {
                InetAddress[] addresses = new InetAddress[0];
                try {
                    addresses = DnsResolvers.JVM_DEFAULT.resolve(redisURI.getHost());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                String cacheIP = addresses[0].getHostAddress();
                HostAndPort finalAddress = hostAndPort;
    
                if (hostAndPort.hostText.equals(cacheIP))
                    finalAddress = HostAndPort.of(redisURI.getHost(), hostAndPort.getPort());
                return finalAddress;
            }
        };
    
        MappingSocketAddressResolver resolver = MappingSocketAddressResolver.create(DnsResolvers.JVM_DEFAULT,mappingFunction);
        return DefaultClientResources.builder().socketAddressResolver(resolver).build();
    }
}
