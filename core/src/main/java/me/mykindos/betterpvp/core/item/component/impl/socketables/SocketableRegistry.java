package me.mykindos.betterpvp.core.item.component.impl.socketables;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
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
public class SocketableRegistry {

    private final Map<NamespacedKey, Socketable> runes = new HashMap<>();
    
    @Inject
    public SocketableRegistry(Core core) {
        final Reflections reflections = new Reflections(getClass().getPackageName());
        for (Class<? extends Socketable> runeClazz : reflections.getSubTypesOf(Socketable.class)) {
            if (runeClazz.isInterface() || Modifier.isAbstract(runeClazz.getModifiers())) {
                continue;
            }

            final Socketable socketableInstance = core.getInjector().getInstance(runeClazz);
            registerRune(socketableInstance);
        }
    }
    
    /**
     * Registers a rune with the registry
     *
     * @param socketable The rune to register
     */
    public void registerRune(@NotNull Socketable socketable) {
        runes.put(socketable.getKey(), socketable);
        log.info("Registered rune: " + socketable.getName()).submit();
    }
    
    /**
     * Gets a rune by its key
     *
     * @param key The rune key
     * @return Optional containing the rune if found
     */
    public Optional<Socketable> getRune(@NotNull NamespacedKey key) {
        return Optional.ofNullable(runes.get(key));
    }
    
    /**
     * Gets all registered runes
     *
     * @return Set of all registered runes
     */
    public Set<Socketable> getAllRunes() {
        return Set.copyOf(runes.values());
    }
} 