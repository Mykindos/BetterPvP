package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.redis.Redis;
import me.mykindos.betterpvp.core.redis.SavedCacheRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <b>WARNING: All methods in this class are blocking.</b>
 */
@Getter
@Singleton
public class ClientRedisLayer extends SavedCacheRepository<Client> {

    public static final long TIME_TO_LIVE = TimeUnit.HOURS.toMillis(1);

    private final ClientObserver observer;

    @Inject
    public ClientRedisLayer(Redis redis, ClientObserver observer) {
        super(redis.createAgent(), "clients");
        this.observer = observer;
    }

    public Optional<Client> getAndUpdate(UUID uuid, String name) {
        final Optional<Client> client = getClient(uuid);
        client.ifPresent(loaded -> {
            if (!loaded.getName().equals(name)) {
                loaded.setName(name);
                save(loaded);
            }
        });
        return client;
    }

    public Optional<Client> getClient(UUID uuid) {
        return this.get(uuid.toString());
    }

    public Optional<Client> getClient(String playerName) {
        return this.collect().stream().filter(client -> client.getName().equalsIgnoreCase(playerName)).findFirst();
    }

    public void save(Client client) {
        this.add(client, TimeUnit.MILLISECONDS.toSeconds(TIME_TO_LIVE));
        this.observer.createUpdate(client.getUniqueId());
    }
}
