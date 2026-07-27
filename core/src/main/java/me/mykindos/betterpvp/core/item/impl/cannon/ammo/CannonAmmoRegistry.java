package me.mykindos.betterpvp.core.item.impl.cannon.ammo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Discovers every concrete {@link CannonAmmo} on the classpath and indexes it twice: by {@link CannonAmmo#id()} (how a
 * chambered round is persisted) and by {@link CannonAmmo#itemKey()} (how a held item is recognised as loadable).
 * <p>
 * Adding a cannonball type is therefore one class - no cannon, listener, or item code has to know about it.
 */
@Singleton
@CustomLog
public class CannonAmmoRegistry {

    private final Map<String, CannonAmmo> byId = new HashMap<>();
    private final Map<String, CannonAmmo> byItemKey = new HashMap<>();
    private final ItemRegistry itemRegistry;

    @Inject
    private CannonAmmoRegistry(@NotNull Core core, @NotNull ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        final Reflections reflections = new Reflections(getClass().getPackageName());
        for (Class<? extends CannonAmmo> clazz : reflections.getSubTypesOf(CannonAmmo.class)) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            final CannonAmmo ammo = core.getInjector().getInstance(clazz);
            byId.put(ammo.id().toLowerCase(Locale.ROOT), ammo);
            byItemKey.put(ammo.itemKey().toLowerCase(Locale.ROOT), ammo);
        }
        log.info("Loaded {} cannon ammo type(s): {}", byId.size(), byId.keySet()).submit();
    }

    /** Resolves a persisted ammo id back to its singleton. */
    public @NotNull Optional<CannonAmmo> byId(@Nullable String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    /** Resolves the ammo an item loads, or empty if the item is not a cannonball. */
    public @NotNull Optional<CannonAmmo> byItem(@Nullable ItemInstance item) {
        if (item == null || item.getBaseItem() == null) {
            return Optional.empty();
        }
        final NamespacedKey key = itemRegistry.getKey(item.getBaseItem());
        return key == null ? Optional.empty() : Optional.ofNullable(byItemKey.get(key.toString().toLowerCase(Locale.ROOT)));
    }

    /** The ammo every cannon accepts when an archetype does not restrict its loadout. */
    public @NotNull CannonAmmo getDefault() {
        return byId.get("explosive");
    }
}
