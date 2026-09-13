package me.mykindos.betterpvp.core.framework.net;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The directory backed by Redis, which is what makes a reservation across servers mean anything.
 * <p>
 * Everything a server advertises carries a time to live and is renewed on a heartbeat, so a server that dies stops
 * renewing and its instances disappear on their own. Nothing has to notice the death or tidy up after it.
 */
@CustomLog
public class RedisSiteDirectory implements SiteDirectory {

    private static final String PREFIX = "bpvp:site:";
    private static final String INSTANCE = PREFIX + "instance:";
    private static final String MEMBERS = PREFIX + "key:";
    private static final String OWNER = PREFIX + "owner:";
    private static final String SERVER = PREFIX + "server:";
    private static final String SERVERS = PREFIX + "servers";
    private static final String ARRIVAL = PREFIX + "arrival:";
    private static final String RESERVED = PREFIX + "reserved:";

    /** Long enough to survive a slow tick, short enough that a dead server's instances go quickly. */
    private static final int ENTRY_TTL_SECONDS = 30;

    /** A player who never arrives should not hold their seats forever. */
    private static final int ARRIVAL_TTL_SECONDS = 60;

    /**
     * Takes seats only if they are there. Redis runs a script to completion before anything else, which is what makes
     * the check and the increment one step rather than two with a gap between them.
     */
    private static final String RESERVE = """
            local occupants = tonumber(redis.call('HGET', KEYS[1], 'occupants') or '0')
            local reserved = tonumber(redis.call('GET', KEYS[2]) or '0')
            local seats = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            if capacity > 0 and occupants + reserved + seats > capacity then
              return 0
            end
            redis.call('INCRBY', KEYS[2], seats)
            redis.call('EXPIRE', KEYS[2], ARGV[3])
            return 1
            """;

    private final Core core;
    private final JedisPool pool;

    public RedisSiteDirectory(@NotNull Core core, @NotNull JedisPool pool) {
        this.core = core;
        this.pool = pool;
    }

    @Override
    public void publish(@NotNull Collection<RemoteInstance> instances) {
        final String server = currentServer();
        final List<RemoteInstance> snapshot = List.copyOf(instances);

        UtilServer.runTaskAsync(core, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.hset(SERVER + server, Map.of("players", String.valueOf(snapshot.stream()
                        .mapToInt(RemoteInstance::getOccupants).sum())));
                jedis.expire(SERVER + server, ENTRY_TTL_SECONDS);
                jedis.sadd(SERVERS, server);

                for (RemoteInstance instance : snapshot) {
                    final String key = INSTANCE + instance.getId();
                    final Map<String, String> fields = new HashMap<>();
                    fields.put("site", instance.getSiteId());
                    fields.put("owner", String.valueOf(instance.getOwnerId()));
                    fields.put("server", instance.getServer());
                    fields.put("world", instance.getWorld());
                    fields.put("ready", String.valueOf(instance.isReady()));

                    // The server holding an instance is the only thing that knows how many people are actually in
                    // it, so it writes that outright. Seats held for parties still travelling live in their own key,
                    // where a heartbeat cannot stamp on them.
                    fields.put("occupants", String.valueOf(instance.getOccupants()));
                    jedis.hset(key, fields);
                    jedis.expire(key, ENTRY_TTL_SECONDS);

                    final String members = MEMBERS + instance.getSiteId() + ":" + instance.getOwnerId();
                    jedis.sadd(members, instance.getId().toString());
                    jedis.expire(members, ENTRY_TTL_SECONDS);
                }
            } catch (Exception exception) {
                log.error("Failed to publish {} instance(s) to the directory", snapshot.size(), exception).submit();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<List<RemoteInstance>> lookup(@NotNull String siteId, long ownerId) {
        return async(jedis -> {
            final String members = MEMBERS + siteId + ":" + ownerId;
            final Set<String> ids = jedis.smembers(members);
            final List<RemoteInstance> found = new ArrayList<>();

            for (String id : ids) {
                final Map<String, String> fields = jedis.hgetAll(INSTANCE + id);
                if (fields.isEmpty()) {
                    // The instance expired, so the id in the set is a leftover. Dropping it here is the only sweep
                    // the set ever gets, and it costs nothing on a lookup that already read it.
                    jedis.srem(members, id);
                    continue;
                }

                // Seats held for parties in transit count against capacity, or two servers would each fill the
                // last place in an instance and both believe they had it.
                final String held = jedis.get(RESERVED + id);
                found.add(new RemoteInstance(UUID.fromString(id), fields.get("site"),
                        Long.parseLong(fields.getOrDefault("owner", "0")), fields.get("server"),
                        fields.get("world"), Boolean.parseBoolean(fields.getOrDefault("ready", "false")),
                        Integer.parseInt(fields.getOrDefault("occupants", "0"))
                                + (held == null ? 0 : Integer.parseInt(held))));
            }

            return found;
        }, List.of());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> reserve(@NotNull UUID instance, int seats, int capacity) {
        return async(jedis -> {
            final Object taken = jedis.eval(RESERVE, List.of(INSTANCE + instance, RESERVED + instance),
                    List.of(String.valueOf(seats), String.valueOf(capacity), String.valueOf(ARRIVAL_TTL_SECONDS)));
            return Long.valueOf(1L).equals(taken);
        }, false);
    }

    @Override
    public void release(@NotNull UUID instance, int seats) {
        UtilServer.runTaskAsync(core, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.decrBy(RESERVED + instance, seats);
            } catch (Exception exception) {
                log.error("Failed to give back {} seat(s) on instance {}", seats, instance, exception).submit();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<String> stickyHost(@NotNull String siteId, long ownerId,
                                                         @NotNull String candidate) {
        return async(jedis -> {
            final String key = OWNER + siteId + ":" + ownerId;
            jedis.setnx(key, candidate);
            final String holder = jedis.get(key);
            return holder == null ? candidate : holder;
        }, candidate);
    }

    @Override
    public @NotNull CompletableFuture<List<String>> serversByLoad() {
        return async(jedis -> {
            final List<String> alive = new ArrayList<>();
            final Map<String, Integer> load = new HashMap<>();

            for (String server : jedis.smembers(SERVERS)) {
                final Map<String, String> fields = jedis.hgetAll(SERVER + server);
                if (fields.isEmpty()) {
                    jedis.srem(SERVERS, server);
                    continue;
                }

                alive.add(server);
                load.put(server, Integer.parseInt(fields.getOrDefault("players", "0")));
            }

            alive.sort(Comparator.comparingInt(server -> load.getOrDefault(server, 0)));
            return alive;
        }, List.of(currentServer()));
    }

    @Override
    public void expect(@NotNull UUID player, @NotNull RemoteInstance instance) {
        UtilServer.runTaskAsync(core, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.hset(ARRIVAL + player, Map.of(
                        "instance", instance.getId().toString(),
                        "site", instance.getSiteId(),
                        "owner", String.valueOf(instance.getOwnerId()),
                        "server", instance.getServer(),
                        "world", instance.getWorld()));
                jedis.expire(ARRIVAL + player, ARRIVAL_TTL_SECONDS);
            } catch (Exception exception) {
                log.error("Failed to record that {} is on their way to {}", player, instance.getId(), exception).submit();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Optional<RemoteInstance>> claimArrival(@NotNull UUID player) {
        return async(jedis -> {
            final String key = ARRIVAL + player;
            final Map<String, String> fields = jedis.hgetAll(key);
            if (fields.isEmpty()) {
                return Optional.empty();
            }

            jedis.del(key);
            return Optional.of(new RemoteInstance(UUID.fromString(fields.get("instance")), fields.get("site"),
                    Long.parseLong(fields.getOrDefault("owner", "0")), fields.get("server"), fields.get("world"),
                    true, 0));
        }, Optional.empty());
    }

    @Override
    public boolean isAvailable() {
        return !pool.isClosed();
    }

    @Override
    public boolean isShared() {
        return true;
    }

    // The pool is shared with the message bus, and closed by the connection that owns it, so there is no close here.

    /**
     * Runs a query off the main thread, answering with {@code fallback} if Redis cannot be reached.
     * <p>
     * A directory that is down should degrade to this server acting alone, not to travel failing outright.
     */
    private <T> @NotNull CompletableFuture<T> async(@NotNull DirectoryQuery<T> query, @NotNull T fallback) {
        final CompletableFuture<T> answered = new CompletableFuture<>();

        UtilServer.runTaskAsync(core, () -> {
            try (Jedis jedis = pool.getResource()) {
                answered.complete(query.run(jedis));
            } catch (Exception exception) {
                log.error("Directory query failed, carrying on as though this server were alone", exception).submit();
                answered.complete(fallback);
            }
        });

        return answered;
    }

    private @NotNull String currentServer() {
        return Core.getCurrentRealm().getServer().getName();
    }

    @FunctionalInterface
    private interface DirectoryQuery<T> {
        T run(@NotNull Jedis jedis) throws Exception;
    }
}
