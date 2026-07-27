package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmo;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmoRegistry;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonProjectile;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonPlaceEvent;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Creates, finds, persists and destroys cannons, and owns the set of shots currently in the air.
 * <p>
 * Cannons are identified by {@link CannonProp#getCannonId()} rather than by their body's entity id, which changes
 * every time the prop re-materializes. Entity-to-cannon lookups go through {@link SceneObjectRegistry}.
 */
@Getter
@Singleton
@CustomLog
@PluginAdapter(value = "ModelEngine", loadMethodName = "ignored")
public class CannonService implements Reloadable {

    private final Core core;
    private final CannonConfig config;
    private final CannonStore store;
    private final CannonArchetypeRegistry archetypes;
    private final CannonAmmoRegistry ammoRegistry;
    private final SceneObjectRegistry sceneRegistry;
    private final Provider<CannonFactory> factory;

    /** Shots in flight, keyed by projectile entity id. */
    private final Map<UUID, CannonProjectile> projectiles = new ConcurrentHashMap<>();

    @Inject
    private CannonService(Core core, CannonConfig config, CannonStore store, CannonArchetypeRegistry archetypes,
                          CannonAmmoRegistry ammoRegistry, SceneObjectRegistry sceneRegistry,
                          Provider<CannonFactory> factory) {
        this.core = core;
        this.config = config;
        this.store = store;
        this.archetypes = archetypes;
        this.ammoRegistry = ammoRegistry;
        this.sceneRegistry = sceneRegistry;
        this.factory = factory;
    }

    /** @return whether {@code entity} is a cannon's body (used to exempt it from targeting, knockback and collisions) */
    public boolean isCannonPart(@NotNull Entity entity) {
        return entity.getPersistentDataContainer()
                .getOrDefault(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "|")
                .equals("cannon");
    }

    /** Resolves the cannon an entity is the body of. */
    public @NotNull Optional<CannonProp> of(@Nullable Entity entity) {
        if (entity == null || !isCannonPart(entity)) {
            return Optional.empty();
        }
        return Optional.ofNullable(sceneRegistry.getObject(entity, CannonProp.class));
    }

    /** Places a new player-owned cannon and persists it. */
    public @NotNull CannonProp spawn(@Nullable UUID placedBy, @NotNull Location location) {
        return spawn(placedBy, location, CannonArchetypeRegistry.STANDARD, CannonProperties.normal(), true);
    }

    public @NotNull CannonProp spawn(@Nullable UUID placedBy, @NotNull Location location,
                                     @NotNull CannonProperties properties) {
        return spawn(placedBy, location, CannonArchetypeRegistry.STANDARD, properties, true);
    }

    /**
     * Places a cannon.
     *
     * @param persistent {@code true} for player-placed cannons that must survive a restart; {@code false} for cannons
     *                   declared by a scene, which the scene rebuilds on every load and which must therefore not leave
     *                   a stale record behind
     */
    public @NotNull CannonProp spawn(@Nullable UUID placedBy, @NotNull Location location, @NotNull String archetypeId,
                                     @NotNull CannonProperties properties, boolean persistent) {
        final CannonArchetype archetype = archetypes.get(archetypeId)
                .orElseGet(() -> archetypes.get(CannonArchetypeRegistry.STANDARD).orElseThrow());
        final CannonProp prop = new CannonProp(factory.get(), this, UUID.randomUUID(), placedBy, archetype,
                properties, config.getCannonHealth());
        register(prop, location, persistent);

        if (persistent) {
            persist(prop);
        }

        final Player player = placedBy == null ? null : Bukkit.getPlayer(placedBy);
        UtilServer.callEvent(new CannonPlaceEvent(prop, location, player));
        return prop;
    }

    /**
     * Builds a cannon without registering it, for callers that own their own scene lifecycle - an island's
     * {@code WorldContent}, say, which pairs the object with its anchor and body factory and hands it to its own
     * loader. The cannon is marked transient, so it is never written to the store: whatever declared it rebuilds it on
     * every load.
     *
     * @see #bodyFactory(CannonProp)
     */
    public @NotNull CannonProp create(@NotNull String archetypeId, @NotNull CannonProperties properties) {
        final CannonArchetype archetype = archetypes.get(archetypeId)
                .orElseGet(() -> archetypes.get(CannonArchetypeRegistry.STANDARD).orElseThrow());
        final CannonProp prop = new CannonProp(factory.get(), this, UUID.randomUUID(), null, archetype,
                properties, config.getCannonHealth());
        prop.markTransient();
        return prop;
    }

    /** The body factory to pair with a {@link #create}d cannon when registering it as chunk-managed. */
    public @NotNull Function<Location, Entity> bodyFactory(@NotNull CannonProp prop) {
        return location -> spawnBody(location, prop);
    }

    /** Rebuilds a cannon from its stored record. */
    public @Nullable CannonProp restore(@NotNull CannonRecord record) {
        final Location location = record.toLocation();
        if (location == null) {
            return null; // world not loaded yet; a later WorldLoadStrategy pass will pick it up
        }
        final CannonArchetype archetype = archetypes.get(record.getArchetypeId()).orElse(null);
        if (archetype == null) {
            log.warn("Cannon {} references unknown archetype '{}'", record.getId(), record.getArchetypeId()).submit();
            return null;
        }

        final CannonProp prop = new CannonProp(factory.get(), this, record.getId(), record.getPlacedBy(), archetype,
                CannonProperties.fromJson(record.getPropertiesJson()), record.getHealth());
        prop.setAmmo(ammoRegistry.byId(record.getAmmoId()).orElse(null));
        prop.getTags().putAll(record.getTags());
        register(prop, location, true);
        return prop;
    }

    /**
     * Registers the cannon as chunk-managed: the framework spawns its golem when the anchor chunk's entities load and
     * rebuilds it on every later cycle. Both the persistent and the scene path go through here, so there is exactly
     * one materialization route regardless of where the cannon came from.
     */
    private void register(@NotNull CannonProp prop, @NotNull Location anchor, boolean persistent) {
        prop.configureMaterialization(anchor, loc -> spawnBody(loc, prop));
        sceneRegistry.register(prop);
        if (!persistent) {
            prop.markTransient();
        }
    }

    private @NotNull Entity spawnBody(@NotNull Location location, @NotNull CannonProp prop) {
        return location.getWorld().spawn(location, IronGolem.class, golem -> {
            golem.getPersistentDataContainer().set(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "cannon");
            if (prop.getPlacedBy() != null) {
                golem.getPersistentDataContainer().set(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID, prop.getPlacedBy());
            }
            golem.setRotation(location.getYaw(), location.getPitch());
        });
    }

    /** Writes the cannon's current position, rotation, chambered round and health to the store. */
    public void persist(@NotNull CannonProp prop) {
        if (prop.isTransientCannon()) {
            return;
        }
        final CannonAmmo ammo = prop.getAmmo();
        store.put(CannonRecord.of(prop.getCannonId(), prop.getLocation(), prop.getArchetype().getId(),
                prop.getProperties(), prop.getPlacedBy(), ammo == null ? null : ammo.id(), prop.getHealth(),
                prop.getTags()));
    }

    /** Destroys a cannon for good: removes its body, unregisters it, and drops its record. */
    public void remove(@NotNull CannonProp prop) {
        if (!prop.isTransientCannon()) {
            store.remove(prop.getCannonId());
        }
        prop.remove();
    }

    /** Maximum health of a cannon. */
    public double getCannonHealth() {
        return config.getCannonHealth();
    }

    /** Minimum delay between cannon placements. */
    public double getSpawnCooldown() {
        return config.getSpawnCooldown();
    }

    /**
     * Pushes configured traits onto every cannon currently standing in the world. Timings resolve through
     * {@link CannonConfig} on each read, but max health is a Bukkit attribute written onto the golem when it
     * materializes, so it has to be re-applied explicitly.
     */
    @Override
    public void reload() {
        for (CannonProp prop : sceneRegistry.getObjects(CannonProp.class)) {
            if (prop.isMaterialized()) {
                prop.refreshFromConfig();
            }
        }
    }

    public void trackProjectile(@NotNull CannonProjectile projectile) {
        projectiles.put(projectile.getEntity().getUniqueId(), projectile);
    }

    public @Nullable CannonProjectile projectileOf(@NotNull Entity entity) {
        return projectiles.get(entity.getUniqueId());
    }
}
