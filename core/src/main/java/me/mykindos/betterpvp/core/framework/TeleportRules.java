package me.mykindos.betterpvp.core.framework;

import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

@Singleton
public class TeleportRules {

    private final Map<String, BiPredicate<LivingEntity, Location>> rules = new ConcurrentHashMap<>();

    public void putRule(@NotNull String key, @NotNull BiPredicate<LivingEntity, Location> rule) {
        rules.put(key, rule);
    }

    public boolean allows(@NotNull LivingEntity entity, @NotNull Location destination) {
        for (BiPredicate<LivingEntity, Location> rule : rules.values()) {
            if (!rule.test(entity, destination)) {
                return false;
            }
        }
        return true;
    }
}
