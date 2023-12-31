package me.mykindos.betterpvp.core.redis;

/**
 * Represents a credentials object for a {@link Redis}.
 */
public class RedisCredentials {

    String password;
    String host;
    int port;

    RedisCredentials(final String password, final String host, final int port) {
        this.password = password;
        this.host = host;
        this.port = port;
    }

    public String getPassword() {
        return this.password;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

}
