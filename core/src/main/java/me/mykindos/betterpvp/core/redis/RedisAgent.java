package me.mykindos.betterpvp.core.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RedisAgent {

    private final JedisPool jedisPool;

    RedisAgent(final JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void useResource(final BiConsumer<Jedis, RedisAgent> consumer) {
        try (final Jedis jedis = this.jedisPool.getResource()) {
            consumer.accept(jedis, this);
        }
    }

    public void useResource(final Consumer<Jedis> consumer) {
        try (final Jedis jedis = this.jedisPool.getResource()) {
            consumer.accept(jedis);
        }
    }

    public <T> T withResource(final Function<Jedis, T> consumer) {
        try (final Jedis jedis = this.jedisPool.getResource()) {
            return consumer.apply(jedis);
        }
    }

}
