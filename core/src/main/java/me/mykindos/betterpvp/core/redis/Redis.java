package me.mykindos.betterpvp.core.redis;

import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Singleton
public class Redis {

    private JedisPool pool;

    public void credentials(final RedisCredentials redisCredentials) {
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

        this.pool = new JedisPool(
                poolConfig,
                redisCredentials.getHost(),
                redisCredentials.getPort(),
                0,
                redisCredentials.getPassword(),
                redisCredentials.getDatabase()
        );
    }

    public void credentials(final FileConfiguration fileConfiguration) {
        final ConfigurationSection section = fileConfiguration.getConfigurationSection("core.redis");
        if (section == null) {
            throw new IllegalArgumentException("The configuration file provided does not contain database details!");
        }

        final RedisCredentials redisInfo = new RedisCredentials(
                section.getString("password"),
                section.getString("host"),
                section.getInt("database"),
                section.getInt("port")
        );

        this.credentials(redisInfo);
    }

    public RedisAgent createAgent() {
        return new RedisAgent(pool);
    }

}
