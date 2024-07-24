package me.mykindos.betterpvp.clans.weapons.impl.cannon.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.IPriorityHandler;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.events.RemoveModelEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.CannonballWeapon;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonAimEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonFuseEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonPlaceEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonShootEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.PreCannonShootEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon.COOLDOWN_LERP_OUT;

@BPvPListener
@Singleton
@PluginAdapter("ModelEngine")
public class CannonListener implements Listener {

    private final Map<Cannon, UUID> fuseMap = new HashMap<>();
    private final Set<Cannon> onCooldown = new HashSet<>();
    private final Map<TNTPrimed, Location> cannonballs = new HashMap<>();

    @Inject
    private Clans clans;

    @Inject
    private ClientManager clientManager;

    @Inject
    private CannonballWeapon cannonballWeapon;

    @Inject
    private CannonManager cannonManager;

    @Inject
    @Config(path = "cannon.entity-collision-explode", defaultValue = "true", configName = "weapons/cannon")
    private boolean entityCollisionExplode;

    @Inject
    @Config(path = "cannon.block-collision-explode", defaultValue = "true", configName = "weapons/cannon")
    private boolean blockCollisionExplode;

    @Inject
    @Config(path = "cannon.cannonball-alive-seconds", defaultValue = "2.0", configName = "weapons/cannon")
    private double cannonballAliveSeconds;

    @Inject
    @Config(path = "cannon.cannonball-velocity-strengh", defaultValue = "1.3", configName = "weapons/cannon")
    private double cannonballVelocityStrength;

    @Inject
    @Config(path = "cannon.cannonball-damage", defaultValue = "15.0", configName = "weapons/cannon")
    private double cannonballDamage;

    @Inject
    @Config(path = "cannon.cannonball-min-damage", defaultValue = "4.0", configName = "weapons/cannon")
    private double cannonballMinDamage;

    @Inject
    @Config(path = "cannon.cannonball-damage-max-radius", defaultValue = "4.0", configName = "weapons/cannon")
    private double cannonballDamageMaxRadius;

    @Inject
    @Config(path = "cannon.cannonball-damage-min-radius", defaultValue = "1.0", configName = "weapons/cannon")
    private double cannonballDamageMinRadius;

    private TNTPrimed spawnCannonball(final @NotNull Cannon cannon, final @NotNull UUID caster) {
        final Location cannonLocation = cannon.getActiveModel().getBone("tnt_start").orElseThrow().getLocation().clone();
        final TNTPrimed cannonball = cannon.getLocation().getWorld().spawn(cannonLocation, TNTPrimed.class);
        cannonball.setSource(Bukkit.getPlayer(caster));
        final Vector direction = cannon.getBackingEntity().getLocation().getDirection();
        direction.multiply(cannonballVelocityStrength);
        cannonball.setVelocity(direction);
        cannonball.setFuseTicks((int) (cannonballAliveSeconds * 20L));
        cannonballs.put(cannonball, cannonball.getLocation());
        cannonball.getPersistentDataContainer().set(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "cannonball");
        cannonball.getPersistentDataContainer().set(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID, caster);
        return cannonball;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CannonPlaceEvent event) {
        UtilServer.runTaskLater(clans, event.getCannon()::updateTag, 4L);
    }

    // Make cannonballs give credit to the player who shot them
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCustomDamage(final PreCustomDamageEvent pre) {
        final CustomDamageEvent event = pre.getCustomDamageEvent();
        //noinspection UnstableApiUsage
        if (event.getDamageSource().getDamageType() == DamageType.PLAYER_EXPLOSION && event.getDamagingEntity() instanceof TNTPrimed tnt) {
            final String type = tnt.getPersistentDataContainer().getOrDefault(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "");
            if (!type.equals("cannonball") || !tnt.getPersistentDataContainer().has(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID)) {
                return;
            }

            final UUID originalOwner = Objects.requireNonNull(tnt.getPersistentDataContainer().get(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID));
            final Player player = Bukkit.getPlayer(originalOwner);
            if (player != null) {
                event.setDamager(player);
                double distance = tnt.getLocation().distance(event.getDamagee().getLocation());
                double damage = getDamage(distance);
                event.setDamage(damage);
                event.setKnockback(false);
                event.addReason("Cannonball");
            }
        }

        // Set sound provider to the cannon if the damagee is a cannon
        this.cannonManager.of(event.getDamagee()).ifPresent(event::setSoundProvider);
    }

    private double getDamage(double distance) {
        double deltaRadius = cannonballDamageMaxRadius - cannonballDamageMinRadius;
        double damage;
        if (distance <= cannonballDamageMinRadius) {
            damage = cannonballDamage;
        } else if (distance > cannonballDamageMaxRadius) {
            damage = 0;
        } else {
            damage = Math.max(cannonballDamage * ((deltaRadius - (distance - cannonballDamageMinRadius)) / deltaRadius), cannonballMinDamage);
        }
        return damage;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReload(final CannonReloadEvent event) {
        final Location location = event.getCannon().getLocation();
        // Play sound and increment cannonballs
        new SoundEffect("littleroom_cannon", "littleroom.cannon.closehatch").play(location);
        event.getCannon().setLoaded(true);
        event.getCannon().updateTag();

        final ActiveModel activeModel = event.getCannon().getActiveModel();
        final IPriorityHandler animationHandler = ((IPriorityHandler) activeModel.getAnimationHandler());
        // Load animation
        animationHandler.playAnimation("load", 0, 0, 0.5, true);
        // Keep loaded after the animation is done
        Objects.requireNonNull(animationHandler.getAnimation("load")).setForceLoopMode(BlueprintAnimation.LoopMode.HOLD);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCannonFuse(final CannonFuseEvent event) {
        event.getCannon().setLastFuseTime(System.currentTimeMillis());
        fuseMap.put(event.getCannon(), event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPreCantShoot(final PreCannonShootEvent event) {
        final TNTPrimed cannonball = spawnCannonball(event.getCannon(), event.getPlayerId());
        new CannonShootEvent(event.getCannon(), cannonball, event.getPlayer()).callEvent();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCannonShoot(final CannonShootEvent event) {
        final Location location = event.getCannon().getLocation();
        // Play sound and decrease cannonballs
        new SoundEffect("littleroom_cannon", "littleroom.cannon.fire", 1f, 2f).play(location);
        new SoundEffect("littleroom_cannon", "littleroom.cannon.ringing", 1f, 1.1f).play(location);
        event.getCannon().setLoaded(false);
        event.getCannon().setLastShotTime(System.currentTimeMillis());

        // Play shoot and stop load animations
        final ActiveModel activeModel = event.getCannon().getActiveModel();
        final IPriorityHandler animationHandler = (IPriorityHandler) activeModel.getAnimationHandler();
        animationHandler.playAnimation("shoot", 0, 0, 1, true);
        Objects.requireNonNull(animationHandler.getAnimation("load")).setForceLoopMode(BlueprintAnimation.LoopMode.ONCE);

        // Play unload sound right after
        UtilServer.runTaskLater(clans, () -> {
            new SoundEffect("littleroom_cannon", "littleroom.cannon.openhatch").play(location);
        }, 2L);

        // Shoot cannonball
        final Location cannonLocation = event.getCannon().getActiveModel().getBone("tnt_start").orElseThrow().getLocation().clone();

        // Smoke particles from cannon
        cannonLocation.add(event.getCannon().getBackingEntity().getLocation().getDirection().multiply(2));
        Particle.EXPLOSION_EMITTER.builder()
                .location(cannonLocation)
                .extra(0)
                .receivers(60)
                .spawn();
        Particle.LARGE_SMOKE.builder()
                .location(cannonLocation)
                .extra(0)
                .count(10)
                .offset(1, 1, 1)
                .receivers(60)
                .spawn();

        onCooldown.add(event.getCannon());
    }

    @UpdateEvent
    public void cannonballTicker() {
        final Iterator<Map.Entry<TNTPrimed, Location>> iterator = cannonballs.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<TNTPrimed, Location> next = iterator.next();
            final TNTPrimed cannonball = next.getKey();
            if (!cannonball.isValid() || cannonball.isDead() || cannonball.getFuseTicks() <= 0) {
                iterator.remove();
                continue;
            }

            final Location lastLocation = next.getValue();
            final Location location = cannonball.getLocation().clone();

            // Collisions
            if (entityCollisionExplode && cannonball.getTicksLived() > 1) {
                final Optional<RayTraceResult> trace = UtilEntity.interpolateCollision(lastLocation,
                        location,
                        0.8f,
                        entity -> !entity.equals(cannonball) && entity instanceof LivingEntity && !this.cannonManager.isCannonPart(entity));

                if (trace.isPresent()) {
                    cannonball.setFuseTicks(0);
                    iterator.remove();
                    continue;
                }
            }

            if (blockCollisionExplode && cannonball.getTicksLived() > 1) {
                final BoundingBox box = cannonball.getBoundingBox().clone().expand(0.1);
                if (UtilLocation.getBoundingBoxCorners(cannonball.getWorld(), box).stream()
                        .anyMatch(loc -> !loc.getBlock().isPassable())) {
                    cannonball.setFuseTicks(0);
                    iterator.remove();
                    continue;
                }
            }

            next.setValue(location);
            // Passive particles
            Particle.SMOKE.builder()
                    .location(location)
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }
    }

    @UpdateEvent
    public void cannonTicker() {
        // Fuse logic
        final Iterator<Map.Entry<Cannon, UUID>> iterator = fuseMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Cannon, UUID> next = iterator.next();
            final Cannon cannon = next.getKey();
            final long fuseTime = cannon.getLastFuseTime();
            cannon.updateTag();

            // Shoot cannon if fuse time has elapsed
            if (UtilTime.elapsed(fuseTime, (long) (this.cannonManager.getFuseSeconds() * 1000L))) {
                iterator.remove();
                final @Nullable Player caster = Bukkit.getPlayer(next.getValue());
                new PreCannonShootEvent(cannon, caster, next.getValue()).callEvent();
                continue;
            }

            // Passive particles
            final Location location = cannon.getActiveModel().getBone("fuse").orElseThrow().getLocation();
            new SoundEffect("littleroom_cannon", "littleroom.cannon.fuse", 1f, 1.3f).play(location);
            Particle.SMALL_FLAME.builder()
                    .location(location)
                    .count(0) // For directional particles, count must be 0
                    .offset(0, 0.15, 0)
                    .extra(0.2)
                    .receivers(60)
                    .spawn();
        }

        // Shoot cooldown logic
        final Iterator<Cannon> cooldownIterator = onCooldown.iterator();
        while (cooldownIterator.hasNext()) {
            final Cannon cannon = cooldownIterator.next();

            // Remove until after 100 mills after the cooldown has elapsed to prevent cooldown from staying in tag
            if (UtilTime.elapsed(cannon.getLastShotTime(), (long) (this.cannonManager.getShootCooldownSeconds() * 1000L) + COOLDOWN_LERP_OUT + 100L)) {
                cooldownIterator.remove();
                continue;
            }

            cannon.updateTag();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAim(final CannonAimEvent event) {
        event.getCannon().rotate(event.getDirection());
        event.getCannon().updateTag();
        new SoundEffect(Sound.BLOCK_IRON_DOOR_CLOSE, 0.3f, 0.5f).play(event.getCannon().getLocation());
    }

    @EventHandler
    public void onInteract(final PlayerInteractAtEntityEvent event) {
        // Don't aim if they are clicking with a cannonball
        final Player player = event.getPlayer();

        this.cannonManager.of(event.getRightClicked()).ifPresent(cannon -> {
            final boolean loaded = cannon.isLoaded();
            if (!loaded && (this.cannonballWeapon.matches(player.getInventory().getItemInMainHand())
                    || this.cannonballWeapon.matches(player.getInventory().getItemInOffHand()))) {
                return; // Allow them to reload if they are holding a cannonball and the cannon is not loaded
            }

            if (!UtilTime.elapsed(cannon.getLastFuseTime(), (long) (this.cannonManager.getFuseSeconds() * 1000L))) {
                return; // Don't aim if the cannon is fused
            }

            if (player.isSneaking()) {
                if (!loaded) {
                    UtilMessage.message(player, "Combat", "This cannon is not loaded! Place a <alt2>Cannonball</alt2> in it to shoot.");
                    SoundEffect.LOW_PITCH_PLING.play(player);
                    return;
                }

                final long expiry = (long) ((this.cannonManager.getShootCooldownSeconds() + this.cannonManager.getFuseSeconds()) * 1000L);
                if (!UtilTime.elapsed(cannon.getLastFuseTime(), expiry)) {
                    UtilMessage.message(player, "Combat", "This cannon is still cooling down!");
                    SoundEffect.LOW_PITCH_PLING.play(player);
                    return;
                }

                final CannonFuseEvent fuseEvent = new CannonFuseEvent(cannon, player);
                fuseEvent.callEvent();
                if (!fuseEvent.isCancelled()) {
                    event.setCancelled(true);
                }

                return; // Attempt to shoot if they are sneaking
            }

            final Vector current = cannon.getBackingEntity().getLocation().getDirection();
            final Vector target = player.getLocation().getDirection();
            final Vector direction = UtilMath.rotateTo(current, target, 0.2f);
            final CannonAimEvent aimEvent = new CannonAimEvent(cannon, player, direction);
            aimEvent.callEvent();
            if (!aimEvent.isCancelled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(final EntityDeathEvent event) {
        this.cannonManager.of(event.getEntity()).ifPresent(cannon -> {
            event.setDroppedExp(0);
            event.getDrops().clear();
            this.cannonManager.remove(cannon);

            // Death effect
            Particle.LARGE_SMOKE.builder()
                    .location(cannon.getLocation())
                    .extra(0)
                    .count(10)
                    .offset(1, 1, 1)
                    .receivers(60)
                    .spawn();

            // Drop cannonball if loaded
            if (cannon.isLoaded()) {
                final Location location = cannon.getLocation();
                location.getWorld().dropItemNaturally(location, this.cannonballWeapon.getItemStack());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(final EntityRemoveFromWorldEvent event) {
        if (!UtilEntity.isRemoved(event.getEntity()) || !UtilEntity.getRemovalReason(event.getEntity()).isDestroy()) {
            return;
        }

        this.cannonManager.of(event.getEntity()).ifPresent(cannon -> {
            this.cannonManager.remove(cannon);

            // Death effect
            Particle.LARGE_SMOKE.builder()
                    .location(cannon.getLocation())
                    .extra(0)
                    .count(10)
                    .offset(1, 1, 1)
                    .receivers(60)
                    .spawn();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModelRemove(final RemoveModelEvent event) {
        final BaseEntity<?> base = event.getModel().getModeledEntity().getBase();
        if (!(base instanceof BukkitEntity bukkitEntity)) {
            return;
        }

        this.cannonManager.of(bukkitEntity.getOriginal()).ifPresent(cannon -> {
            this.cannonManager.remove(cannon);

            // Death effect
            Particle.LARGE_SMOKE.builder()
                    .location(cannon.getLocation())
                    .extra(0)
                    .count(10)
                    .offset(1, 1, 1)
                    .receivers(60)
                    .spawn();
        });
    }

    @EventHandler
    public void onEntityVelocity(final CustomEntityVelocityEvent event) {
        if (this.cannonManager.isCannonPart(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTag(final CustomDamageEvent event) {
        try {
            if (event.getDamagee().getHealth() > 0) {
                this.cannonManager.of(event.getDamagee()).ifPresent(Cannon::updateTag);
            }
        } catch (IllegalStateException ignored) {
            // Ignore if the cannon has no healthbar, means they died
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCannonDamage(final CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.SUFFOCATION || !this.cannonManager.isCannonPart(event.getDamagee())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onTarget(final EntityTargetEvent event) {
        if (event.getTarget() == null) {
            return;
        }

        if (this.cannonManager.isCannonPart(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLoad(ModelRegistrationEvent event) {
        if (event.getPhase() != ModelGenerator.Phase.FINISHED) {
            return;
        }
        UtilServer.runTask(clans, () -> this.cannonManager.load());
    }


}
