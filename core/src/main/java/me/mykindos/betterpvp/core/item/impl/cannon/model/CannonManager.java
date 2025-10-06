package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonPlaceEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Singleton
@PluginAdapter(value = "ModelEngine", loadMethodName = "ignored")
public class CannonManager extends Manager<String, Cannon> {

    @Inject
    @Config(path = "cannon.fuse-seconds", defaultValue = "2.5", configName = "items/weapon")
    private double fuseSeconds;

    @Inject
    @Config(path = "cannon.shoot-cooldown", defaultValue = "30.0", configName = "items/weapon")
    private double shootCooldownSeconds;

    @Inject
    @Config(path = "cannon.spawn-cooldown", defaultValue = "30.0", configName = "items/weapon")
    private double spawnCooldown;

    @Inject
    @Config(path = "cannon.health", defaultValue = "200.0", configName = "items/weapon")
    private double cannonHealth;

    private final Core core;

    @Inject
    public CannonManager(Core core) {
        this.core = core;
    }

    public boolean isCannonPart(@NotNull Entity entity) {
        final String type = entity.getPersistentDataContainer().getOrDefault(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "|");
        return type.equals("cannon") || type.equals("cannon_health_bar");
    }

    public void load() {
        for (World world : Bukkit.getServer().getWorlds()) {
            for (IronGolem golem : world.getEntitiesByClass(IronGolem.class)) {
                if (isCannonPart(golem)) {
                    // Load properties from PDC or use default
                    final PersistentDataContainer pdc = golem.getPersistentDataContainer();
                    final CannonProperties properties;
                    if (pdc.has(CoreNamespaceKeys.CANNON_PROPERTIES, PersistentDataType.STRING)) {
                        final String json = pdc.get(CoreNamespaceKeys.CANNON_PROPERTIES, PersistentDataType.STRING);
                        properties = CannonProperties.fromJson(json);
                    } else {
                        properties = CannonProperties.normal();
                    }
                    setupEntity(properties, golem);
                    ModelEngineAPI.getOrCreateModeledEntity(golem, modeledEntity -> setupModel(properties, modeledEntity));
                }
            }
        }
    }

    public void remove(@NotNull Cannon cannon) {
        removeObject(cannon.getUuid().toString());
        final TextDisplay healthBar = cannon.getHealthBar();
        if (healthBar != null && healthBar.isValid()) {
            healthBar.remove();
        }

        final TextDisplay info = cannon.getInstructions();
        if (info != null && info.isValid()) {
            info.remove();
        }

        if (!cannon.getModeledEntity().isDestroyed()) {
            cannon.getModeledEntity().destroy();
        }

        final IronGolem backingEntity = cannon.getBackingEntity();
        if (backingEntity.isValid()) {
            backingEntity.remove();
        }
    }

    private ActiveModel setupModel(@NotNull CannonProperties properties, @NotNull final ModeledEntity modeledEntity) {
        // Setup model
        final ActiveModel activeModel = ModelEngineAPI.createActiveModel("cannon");
        activeModel.setScale(properties.getSize());
        activeModel.setHitboxScale(properties.getSize() + 0.4);
        activeModel.setShadowVisible(true);
        // Attach model and setup entity
        modeledEntity.addModel(activeModel, true);
        modeledEntity.setBaseEntityVisible(false);
        modeledEntity.setModelRotationLocked(false);
        modeledEntity.setSaved(true);
        return activeModel;
    }

    private void setupEntity(@NotNull CannonProperties cannonProperties, @NotNull IronGolem golem) {
        golem.setAggressive(false);
        golem.setPersistent(true);
        Bukkit.getMobGoals().removeAllGoals(golem);
        golem.setRemoveWhenFarAway(false);
        golem.setGravity(true);
        golem.customName(Component.text("Cannon"));
        golem.setCustomNameVisible(false);
        golem.setAware(false);
        if (!cannonProperties.isMovable()) {
            golem.setGravity(false);
            Objects.requireNonNull(golem.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(Integer.MAX_VALUE);
            Objects.requireNonNull(golem.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE)).setBaseValue(Integer.MAX_VALUE);
            Objects.requireNonNull(golem.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0);
            Objects.requireNonNull(golem.getAttribute(Attribute.GRAVITY)).setBaseValue(0);
        }
        golem.setVisualFire(TriState.FALSE);
        golem.setCollidable(false);
        Objects.requireNonNull(golem.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0D);
        Objects.requireNonNull(golem.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(cannonHealth);
        golem.setMetadata("PlayerSpawned", new FixedMetadataValue(core, true));
    }

    public Optional<Cannon> of(@NotNull Entity entity) {
        return getObject(entity.getUniqueId()).or(() -> {
            if (!(entity instanceof IronGolem golem)) {
                return Optional.empty();
            }

            final PersistentDataContainer pdc = golem.getPersistentDataContainer();
            if (!pdc.getOrDefault(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "").equals("cannon")) {
                return Optional.empty();
            }

            if (!pdc.has(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID)) {
                throw new IllegalStateException("Cannon entity does not have an original owner key!");
            }

            // Load properties from PDC or use default
            final CannonProperties properties;
            if (pdc.has(CoreNamespaceKeys.CANNON_PROPERTIES, PersistentDataType.STRING)) {
                final String json = pdc.get(CoreNamespaceKeys.CANNON_PROPERTIES, PersistentDataType.STRING);
                properties = CannonProperties.fromJson(json);
            } else {
                properties = CannonProperties.normal();
            }
            setupEntity(properties, golem);
            final UUID placedBy = Objects.requireNonNull(pdc.get(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID));
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(golem);
            final ActiveModel activeModel;
            if (modeledEntity != null && modeledEntity.isDestroyed()) {
                return Optional.empty();
            } else if (modeledEntity == null) {
                modeledEntity = ModelEngineAPI.createModeledEntity(golem);
                activeModel = setupModel(properties, modeledEntity);
            } else {
                activeModel = modeledEntity.getModel("cannon").orElseThrow();
            }

            final boolean loaded = pdc.getOrDefault(CoreNamespaceKeys.CANNON_LOADED, PersistentDataType.BOOLEAN, false);
            final Cannon created = new Cannon(this, entity.getUniqueId(), placedBy, golem, modeledEntity, activeModel, loaded, properties);
            addObject(entity.getUniqueId().toString(), created);
            return Optional.of(created);
        });
    }

    public Cannon spawn(@Nullable UUID placedBy, @NotNull Location location) {
        return spawn(placedBy, location, CannonProperties.normal());
    }

    public Cannon spawn(@Nullable UUID placedBy, @NotNull Location location, @NotNull CannonProperties properties) {
        // entity
        final IronGolem entity = location.getWorld().spawn(location, IronGolem.class, ent -> {
            ent.getPersistentDataContainer().set(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "cannon");
            if (placedBy != null) {
                ent.getPersistentDataContainer().set(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID, placedBy);
            }
            // Save properties to PDC
            ent.getPersistentDataContainer().set(CoreNamespaceKeys.CANNON_PROPERTIES, PersistentDataType.STRING, properties.toJson());
            setupEntity(properties, ent);
            ent.setRotation(location.getYaw(), location.getPitch());
            ent.setHealth(cannonHealth);
        });

        final ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity);
        final ActiveModel activeModel = setupModel(properties, modeledEntity);
        final Cannon cannon = new Cannon(this,
                entity.getUniqueId(),
                placedBy,
                entity,
                modeledEntity,
                activeModel,
                false,
                properties);
        addObject(entity.getUniqueId().toString(), cannon);
        final Player player = placedBy == null ? null : Bukkit.getPlayer(placedBy);
        UtilServer.callEvent(new CannonPlaceEvent(cannon, location, player));
        return cannon;
    }
}
