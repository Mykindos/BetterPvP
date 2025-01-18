package me.mykindos.betterpvp.core.framework.profiles;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerProfiles {

    public static final Cache<UUID, PlayerProfile> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    private static final ReentrantLock cacheLock = new ReentrantLock();

    public static PlayerProfile computeIfAbsent(UUID uuid, java.util.function.Function<? super UUID, ? extends PlayerProfile> mappingFunction) {
        cacheLock.lock();
        try {
            return CACHE.get(uuid, mappingFunction);
        } finally {
            cacheLock.unlock();
        }
    }
}
