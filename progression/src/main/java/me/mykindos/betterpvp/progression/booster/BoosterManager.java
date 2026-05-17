package me.mykindos.betterpvp.progression.booster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class BoosterManager {

    private final BoosterRepository repository;
    
    @Getter
    private final Map<UUID, Long> activeBoosters = new HashMap<>();

    @Inject
    public BoosterManager(BoosterRepository repository) {
        this.repository = repository;
    }

    public void activateBooster(UUID uuid, long durationMillis) {
        long currentExpiry = activeBoosters.getOrDefault(uuid, System.currentTimeMillis());
        if (currentExpiry < System.currentTimeMillis()) {
            currentExpiry = System.currentTimeMillis();
        }
        
        long newExpiry = currentExpiry + durationMillis;
        activeBoosters.put(uuid, newExpiry);
        repository.saveBooster(uuid, newExpiry);
    }

    public boolean hasBooster(UUID uuid) {
        Long expiry = activeBoosters.get(uuid);
        if (expiry == null) return false;
        if (expiry < System.currentTimeMillis()) {
            activeBoosters.remove(uuid);
            return false;
        }
        return true;
    }

    public long getRemainingTime(UUID uuid) {
        Long expiry = activeBoosters.get(uuid);
        if (expiry == null) return 0;
        return Math.max(0, expiry - System.currentTimeMillis());
    }
}
