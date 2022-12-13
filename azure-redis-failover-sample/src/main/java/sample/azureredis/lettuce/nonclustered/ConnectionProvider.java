package sample.azureredis.lettuce.nonclustered;

import io.lettuce.core.*;
import io.lettuce.core.api.*;
import io.lettuce.core.api.sync.*;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.TimedSemaphore;
import sample.azureredis.lettuce.shared.ErrorHelper;
import sample.azureredis.lettuce.shared.MultiPasswordCredentials;

class ConnectionProvider
{
    private boolean initialized = false;
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private MultiPasswordCredentials credentials;
    private RedisURI redisURI;
    private TimedSemaphore passwordSwapSemaphore;

    public ConnectionProvider(String hostname, int port, String password, String secondaryPassword) {
        this.credentials = new MultiPasswordCredentials(password, secondaryPassword);
        this.redisURI = RedisURI.Builder.redis(hostname)
            .withSsl(true)
            .withAuthentication(RedisCredentialsProvider.from(() -> credentials))
            .withClientName("LettuceClient")
            .withPort(port)
            .withTimeout(Duration.ofSeconds(5))
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

        try {
            if (passwordSwapSemaphore != null) {
                passwordSwapSemaphore.shutdown();
            }
        }
        finally {
            passwordSwapSemaphore = null;
        }
    }

    private synchronized void init() {
        if (!initialized) {
            passwordSwapSemaphore = new TimedSemaphore(5, TimeUnit.SECONDS, 1);

            redisClient = RedisClient.create(redisURI);
            setClientOptions(redisClient);

            // Subscribe to reconnect failure events to handle auth failure
            EventBus eventBus = redisClient.getResources().eventBus();
            eventBus.get().subscribe((e) -> {
                if (e instanceof ReconnectFailedEvent) {
                    ReconnectFailedEvent event = (ReconnectFailedEvent) e;
                    if (ErrorHelper.causedByAuthFailure(event.getCause())) {
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
        // Limit frequency of password swaps to prevent potential (but unlikely) race conditions
        if (passwordSwapSemaphore.tryAcquire()) {
            System.out.println("Auth failure. Switching to alternate password.");
            credentials.swapPassword();
        }
        else {
            System.out.println("Auth failure. Retrying...");
        }
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
