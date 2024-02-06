package me.mykindos.betterpvp.core.client.repository;

import com.github.benmanes.caffeine.cache.Expiry;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.checkerframework.checker.index.qual.NonNegative;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class ClientExpiry<T> implements Expiry<UUID, T> {

    @Override
    public long expireAfterCreate(UUID key, Object value, long currentTime) {
        return isOnline(key) ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.toNanos(ClientManager.TIME_TO_LIVE);
    }

    @Override
    public long expireAfterUpdate(UUID key, Object value, long currentTime, @NonNegative long currentDuration) {
        return isOnline(key) ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.toNanos(ClientManager.TIME_TO_LIVE);
    }

    @Override
    public long expireAfterRead(UUID key, Object value, long currentTime, @NonNegative long currentDuration) {
        return isOnline(key) ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.toNanos(ClientManager.TIME_TO_LIVE);
    }

    private boolean isOnline(UUID client) {
        return Bukkit.getPlayer(client) != null;
    }
}
