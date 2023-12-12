package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.redis.RedisAgent;
import me.mykindos.betterpvp.core.redis.Redis;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
@Singleton
public class ClientObserver extends JedisPubSub {

    public static final String CHANNEL = "clients";
    private final RedisAgent agent;
    private final List<Consumer<UUID>> listeners = new ArrayList<>();

    @Inject
    public ClientObserver(Core core, Redis redis) {
        this.agent = redis.createAgent();
        UtilServer.runTaskAsync(core, () -> redis.createAgent().useResource(jedis -> jedis.subscribe(this, CHANNEL)));
    }

    public static String encode(UUID uuid) {
        return "UPDATE_CLIENT " + uuid.toString();
    }

    public static Optional<UUID> decode(String message) {
        if (!message.startsWith("UPDATE_CLIENT ")) {
            return Optional.empty();
        }

        final String uuid = message.substring("UPDATE_CLIENT ".length());
        return Optional.of(UUID.fromString(uuid));
    }

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        final Optional<UUID> uuidOpt = decode(message);
        uuidOpt.ifPresent(uuid -> listeners.forEach(listener -> listener.accept(uuid)));
    }

    protected void createUpdate(UUID uuid) {
        this.agent.useResource(jedis -> jedis.publish(CHANNEL, encode(uuid)));
    }

    public void register(Consumer<UUID> listener) {
        this.listeners.add(listener);
    }
}
