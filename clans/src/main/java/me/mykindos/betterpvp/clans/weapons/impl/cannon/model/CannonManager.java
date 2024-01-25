package me.mykindos.betterpvp.clans.weapons.impl.cannon.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.IPriorityHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Singleton
@PluginAdapter("ModelEngine")
public class CannonManager extends Manager<Cannon> {

    @Inject
    @Config(path = "cannon.fuse-seconds", defaultValue = "2.5", configName = "weapons/cannon")
    private double fuseSeconds;

    @Inject
    @Config(path = "cannon.shoot-cooldown", defaultValue = "30.0", configName = "weapons/cannon")
    private double shootCooldownSeconds;

    @Inject
    @Config(path = "cannon.spawn-cooldown", defaultValue = "30.0", configName = "weapons/cannon")
    private double spawnCooldown;

    @Inject
    @Config(path = "cannon.health", defaultValue = "200.0", configName = "weapons/cannon")
    private double cannonHealth;

    public void remove(@NotNull Cannon cannon) {
        if (cannon.getHealthBar().isValid() && !cannon.getHealthBar().isDead()) {
            cannon.getHealthBar().remove();
        }
        if (!cannon.getModeledEntity().isDestroyed()) {
            cannon.getModeledEntity().destroy();
        }
        removeObject(cannon.getUuid().toString());
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

            if (!pdc.has(ClansNamespacedKeys.CANNON_NAMETAG, CustomDataType.UUID)) {
                throw new IllegalStateException("Cannon entity does not have a nametag key!");
            }

            final UUID placedBy = Objects.requireNonNull(pdc.get(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID));
            final ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(golem);;
            if (modeledEntity == null || modeledEntity.isDestroyed()) {
                return Optional.empty();
            }

            final boolean loaded = pdc.getOrDefault(ClansNamespacedKeys.CANNON_LOADED, PersistentDataType.BOOLEAN, false);
            final ActiveModel activeModel = modeledEntity.getModel("cannon").orElseThrow(() -> new IllegalStateException("Cannon entity does not have a cannon model!"));
            final UUID nametagId = Objects.requireNonNull(pdc.get(ClansNamespacedKeys.CANNON_NAMETAG, CustomDataType.UUID));
            final Optional<TextDisplay> healthBarOpt = Optional.ofNullable(entity.getWorld().getEntity(nametagId)).map(TextDisplay.class::cast);
            if (healthBarOpt.isEmpty()) {
                throw new IllegalStateException("Cannon nametag was despawned!");
            }

            final Optional<Cannon> created = Optional.of(new Cannon(
                    this,
                    entity.getUniqueId(),
                    placedBy,
                    golem,
                    modeledEntity,
                    activeModel,
                    healthBarOpt.get(),
                    loaded
            ));
            addObject(entity.getUniqueId().toString(), created.get());
            setupEntity(golem);
            return created;
        });
    }

    private void setupEntity(@NotNull IronGolem golem) {
        golem.setAggressive(false);
        golem.setPersistent(true);
        golem.setGravity(true);
        golem.customName(Component.text("Cannon"));
        golem.setCustomNameVisible(false);
        golem.setAware(false);
        golem.setVisualFire(false);
        golem.setCollidable(false);
        golem.setSilent(true);
    }

    public boolean isCannonPart(@NotNull Entity entity) {
        final String type = entity.getPersistentDataContainer().getOrDefault(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "|");
        return type.equals("cannon") || type.equals("cannon_health_bar");
    }

    public Cannon spawn(@NotNull UUID placedBy, @NotNull Location location) {
        // name tags
        final TextDisplay healthBar = location.getWorld().spawn(location, TextDisplay.class, ent -> {
            ent.getPersistentDataContainer().set(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "cannon_health_bar");
            ent.setAlignment(TextDisplay.TextAlignment.CENTER);
            ent.setViewRange(1);
            ent.setBackgroundColor(Color.fromARGB(0, 255, 255, 255));
            ent.customName(Component.text("Cannon"));
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setShadowRadius(1.2f);
            ent.setShadowStrength(0.4f);
            ent.setPersistent(true);
            ent.setTransformation(new Transformation(
                    new Vector3f(0, 3f, 0),
                    new AxisAngle4f(),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f()
            ));
        });

        // entity
        final IronGolem entity = location.getWorld().spawn(location, IronGolem.class, ent -> {
            Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0D);
            Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(cannonHealth);
            ent.setHealth(cannonHealth);
            ent.setRotation(location.getYaw(), location.getPitch());
            ent.getPersistentDataContainer().set(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "cannon");
            ent.getPersistentDataContainer().set(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID, placedBy);
            ent.getPersistentDataContainer().set(ClansNamespacedKeys.CANNON_NAMETAG, CustomDataType.UUID, healthBar.getUniqueId());
            setupEntity(ent);
        });

        // model engine
        final ActiveModel activeModel = ModelEngineAPI.createActiveModel("cannon");
        activeModel.setHitboxScale(1.3D);
        activeModel.setShadowVisible(true);
        final IPriorityHandler animationHandler = (IPriorityHandler) activeModel.getAnimationHandler();
        animationHandler.playState(ModelState.IDLE);
        final ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity, ent -> {
            ent.addModel(activeModel, true);
            ent.setBaseEntityVisible(false);
            ent.setModelRotationLocked(false);
            ent.setSaved(true);
        });

        return new Cannon(this,
                entity.getUniqueId(),
                placedBy,
                entity,
                modeledEntity,
                activeModel,
                healthBar,
                false);
    }

    public void load() {
        for (World world : Bukkit.getServer().getWorlds()) {
            for (IronGolem golem : world.getEntitiesByClass(IronGolem.class)) {
                if (isCannonPart(golem)) {
                    setupEntity(golem);
                }
            }
        }
    }
}
