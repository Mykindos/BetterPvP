package me.mykindos.betterpvp.core.redis;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Singleton
@CustomLog
public class Redis {

    private JedisPool pool;

    public void credentials(final RedisCredentials redisCredentials) {
        Preconditions.checkNotNull(redisCredentials, "Redis credentials cannot be null!");
        Preconditions.checkState(pool == null, "Redis credentials have already been set!");

        final JedisPoolConfig poolConfig = getJedisPoolConfig();
        if (redisCredentials.getPassword() == null || redisCredentials.getPassword().isEmpty()) {
            // Passwordless redis
            this.pool = new JedisPool(
                    poolConfig,
                    redisCredentials.getHost(),
                    redisCredentials.getPort(),
                    0
            );
        } else {
            // Passworded redis
            this.pool = new JedisPool(
                    poolConfig,
                    redisCredentials.getHost(),
                    redisCredentials.getPort(),
                    0,
                    redisCredentials.getPassword()
            );
        }

        log.info("Redis has been enabled.");
    }

    @NotNull
    private static JedisPoolConfig getJedisPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    public void credentials(final FileConfiguration fileConfiguration) {
        final ConfigurationSection section = fileConfiguration.getConfigurationSection("core.redis");
        if (section == null) {
            throw new IllegalArgumentException("The configuration file provided does not contain database details!");
        }

        if (!section.getBoolean("enabled")) {
            log.warn("Redis has been disabled in the configuration file. Cross-server functionality will not work.");
            return;
        }

        final RedisCredentials redisInfo = new RedisCredentials(
                section.getString("password"),
                section.getString("host"),
                section.getInt("port")
        );

        this.credentials(redisInfo);
    }

    public void shutdown() {
        if (pool != null) {
            pool.close();
        }
    }

    public RedisAgent createAgent() {
        return new RedisAgent(pool);
    }

    public boolean isEnabled() {
        return pool != null;
    }

}
