package sample.azureredis.lettuce.shared;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisCommandExecutionException;

public class ErrorHelper {
    public static boolean causedByAuthFailure(Throwable throwable) {
        Throwable t = throwable;
        while (t != null) {
            if (isAuthError(t)) {
                return true;
            }

            if (t.getSuppressed() != null) {
                for (Throwable suppressed : t.getSuppressed()) {
                    if (isAuthError(suppressed)) {
                        return true;
                    }
                }
            }

            t = t.getCause();
        }

        return false;
    }

    private static boolean isAuthError(Throwable t) {
        return t instanceof RedisConnectionException
            || t instanceof RedisCommandExecutionException
            && t.getMessage().contains("WRONGPASS");
    }
}