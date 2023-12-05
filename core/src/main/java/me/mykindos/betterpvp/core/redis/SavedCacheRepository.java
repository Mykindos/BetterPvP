package me.mykindos.betterpvp.core.redis;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonIOException;
import lombok.Getter;
import me.mykindos.betterpvp.core.serialization.Serialization;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class SavedCacheRepository<T extends CacheObject> {

    @Getter
    protected final String name;
    @Getter
    protected final RedisAgent agent;
    protected final TypeToken<T> type;

    protected SavedCacheRepository(final RedisAgent agent, final String name) {
        this.type = new TypeToken<>(getClass()) {};
        this.agent = agent;
        this.name = name;
    }

    protected String getStoreKey() {
        return "betterpvp:" + this.getName();
    }

    protected String getStoreKey(final String dataKey) {
        return this.getStoreKey() + ":" + dataKey;
    }

    protected String getStoreKey(final T data) {
        return this.getStoreKey(data.getKey());
    }

    protected void add(final T object) {
        this.add(object, TimeUnit.DAYS.toSeconds(365 * 2L)); // expires in 2 years
    }

    protected void add(final T object, final long expirySeconds) {
        this.agent.useResource(jedis -> {
            final String json = this.serialize(object);

            final Transaction pipeline = jedis.multi();
            final String dataKey = this.getStoreKey(object);
            pipeline.set(dataKey, json);
            pipeline.zadd(this.getStoreKey(), this.getCurrentSecond() + (double) expirySeconds, dataKey);
            pipeline.exec();
        });
    }

    protected void remove(final T object) {
        this.remove(object.getKey());
    }

    protected void remove(final String dataKey) {
        this.removeMulti(dataKey);
    }

    protected void removeMulti(final String... dataKeys) {
        this.agent.useResource(jedis -> {
            final Transaction transaction = jedis.multi();
            for (final String storeKey : dataKeys) {
                transaction.del(storeKey);
                transaction.zrem(this.getStoreKey(), storeKey);
            }
            transaction.exec();
        });
    }

    protected void flush() {
        this.agent.useResource(jedis -> {
            final String currentTime = String.valueOf(this.getCurrentSecond());
            final List<String> dead = jedis.zrangeByScore(this.getStoreKey(), "-inf", currentTime).stream().map(key -> {
                final String[] split = key.split(":");
                return split[split.length - 1];
            }).toList();
            this.removeMulti(dead.toArray(new String[0]));
        });
    }

    protected List<T> collect() {
        return this.agent.withResource(jedis -> this.get(this.getAlive().stream().map(key -> {
            final String[] split = key.split(":");
            return split[split.length - 1];
        }).toList()));
    }

    protected List<T> get(final List<String> dataKeys) {
        return this.agent.withResource(jedis -> {
            final ArrayList<T> result = new ArrayList<>();
            final Pipeline pipeline = jedis.pipelined();
            final List<Response<String>> responses = new ArrayList<>();

            for (final String dataKey : dataKeys) {
                if (!this.exists(dataKey)) {
                    continue;
                }

                responses.add(pipeline.get(this.getStoreKey(dataKey)));
            }

            pipeline.sync();
            for (final Response<String> jsonResponse : responses) {
                final String json = jsonResponse.get();
                if (json == null) {
                    continue;
                }

                result.add(this.deserialize(json));
            }

            return result;
        });
    }

    protected Optional<T> get(final String dataKey) {
        return this.agent.withResource(jedis -> {
            if (!this.exists(dataKey)) {
                return Optional.empty();
            }

            final String key = this.getStoreKey(dataKey);
            final String json = jedis.get(key);
            return Optional.of(this.deserialize(json));
        });
    }

    protected List<String> getAlive() {
        this.flush();
        return this.agent.withResource(
                jedis -> jedis.zrangeByScore(this.getStoreKey(), "(" + this.getCurrentSecond(), "+inf"));
    }

    protected boolean exists(final String dataKey) {
        return this.agent.withResource(jedis -> jedis.exists(this.getStoreKey(dataKey)));
    }

    protected long getCurrentSecond() {
        return Instant.now().getEpochSecond();
    }

    protected String serialize(final T object) {
        try {
            return Serialization.GSON.toJson(object, this.type.getType());
        } catch (final JsonIOException e) {
            throw new RuntimeException(e);
        }
    }

    protected T deserialize(final String json) {
        try {
            return Serialization.GSON.fromJson(json, this.type.getType());
        } catch (final JsonIOException e) {
            throw new RuntimeException(e);
        }
    }

}
