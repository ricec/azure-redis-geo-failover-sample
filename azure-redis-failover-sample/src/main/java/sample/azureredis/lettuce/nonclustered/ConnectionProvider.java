package sample.azureredis.lettuce.nonclustered;

import io.lettuce.core.*;
import io.lettuce.core.api.*;
import io.lettuce.core.api.sync.*;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import sample.azureredis.lettuce.shared.ErrorHelper;
import sample.azureredis.lettuce.shared.MultiPasswordCredentials;

class ConnectionProvider
{
    private boolean initialized = false;
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private MultiPasswordCredentials credentials;
    private RedisURI redisURI;

    public ConnectionProvider(String hostname, int port, String password, String secondaryPassword) {
        this.credentials = new MultiPasswordCredentials(password, secondaryPassword);
        this.redisURI = RedisURI.Builder.redis(hostname)
            .withSsl(true)
            .withAuthentication(RedisCredentialsProvider.from(() -> credentials))
            .withClientName("LettuceClient")
            .withPort(port)
            .build();
    }

    public StatefulRedisConnection<String, String> getConnection() {
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
            redisClient = RedisClient.create(redisURI);
            setClientOptions(redisClient);

            // Subscribe to reconnect failure events to handle auth failure
            EventBus eventBus = redisClient.getResources().eventBus();
            eventBus.get().subscribe((e) -> {
                if (e instanceof ReconnectFailedEvent)
                {
                    ReconnectFailedEvent event = (ReconnectFailedEvent) e;
                    if (ErrorHelper.causedByAuthFailure(event.getCause()) && event.getAttempt() == 1) {
                        handleAuthFailure();
                    }
                }
            });

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

    // Sets ClientOptions in accordance with the best-practices doc here:
    // https://github.com/Azure/AzureCacheForRedis/blob/main/Lettuce%20Best%20Practices.md
    private void setClientOptions(RedisClient redisClient) {
        redisClient.setOptions(
            ClientOptions.builder()
                .socketOptions(SocketOptions.builder().keepAlive(true).build())
                .build());
    }
}
