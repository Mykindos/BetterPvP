package me.mykindos.betterpvp.core.framework.net;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * The one connection to Redis, shared by everything that needs it.
 * <p>
 * Connecting is tried once, at the first thing that asks. A pool hands out connections lazily, so a round trip is
 * made here to find out whether Redis is actually there: without it an unreachable server would look like a working
 * one right up until nothing worked.
 */
@Singleton
@CustomLog
public class RedisConnection {

    private final Core core;

    @Getter
    private JedisPool pool;

    private boolean attempted;

    @Inject
    public RedisConnection(@NotNull Core core) {
        this.core = core;
    }

    /** Whether Redis is configured and answered. Everything that can work without it asks this first. */
    public synchronized boolean isUsable() {
        if (!attempted) {
            attempted = true;
            connect();
        }
        return pool != null && !pool.isClosed();
    }

    public @NotNull String channel() {
        return core.getConfig().getOrSaveString("core.redis.channel", "betterpvp:bus");
    }

    public synchronized void close() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    private void connect() {
        if (!core.getConfig().getOrSaveObject("core.redis.enabled", false, Boolean.class)) {
            return;
        }

        final String host = core.getConfig().getOrSaveString("core.redis.host", "127.0.0.1");
        final int port = core.getConfig().getOrSaveObject("core.redis.port", 6379, Integer.class);
        final String password = core.getConfig().getOrSaveString("core.redis.password", "");

        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(16);
        config.setMaxIdle(8);
        config.setTestOnBorrow(true);

        final JedisPool candidate = password.isBlank()
                ? new JedisPool(config, host, port)
                : new JedisPool(config, host, port, 2000, password);

        try (Jedis jedis = candidate.getResource()) {
            jedis.ping();
            pool = candidate;
            log.info("Connected to Redis at {}:{}", host, port).submit();
        } catch (Exception exception) {
            candidate.close();
            log.error("Could not reach Redis at {}:{}, carrying on without it", host, port, exception).submit();
        }
    }
}
