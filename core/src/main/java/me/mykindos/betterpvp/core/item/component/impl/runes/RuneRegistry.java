package me.mykindos.betterpvp.core.item.component.impl.runes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry that tracks all registered runes.
 * Used for serialization and deserialization of runes.
 */
@Singleton
@CustomLog
public class RuneRegistry {

    private final Map<NamespacedKey, Rune> runes = new HashMap<>();
    
    @Inject
    public RuneRegistry() {
        // Empty constructor for Guice
    }
    
    /**
     * Registers a rune with the registry
     *
     * @param rune The rune to register
     */
    public void registerRune(@NotNull Rune rune) {
        runes.put(rune.getKey(), rune);
        log.info("Registered rune: " + rune.getName()).submit();
    }
    
    /**
     * Gets a rune by its key
     *
     * @param key The rune key
     * @return Optional containing the rune if found
     */
    public Optional<Rune> getRune(@NotNull NamespacedKey key) {
        return Optional.ofNullable(runes.get(key));
    }
    
    /**
     * Gets all registered runes
     *
     * @return Set of all registered runes
     */
    public Set<Rune> getAllRunes() {
        return Set.copyOf(runes.values());
    }
} 