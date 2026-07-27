package me.mykindos.betterpvp.core.item.impl.cannon.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.IPriorityHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import io.papermc.paper.event.entity.EntityMoveEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmo;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmoRegistry;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonProjectile;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonAimEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonPlaceEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonShootEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonballExplodeEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonService;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonState;
import me.mykindos.betterpvp.core.item.impl.cannon.ride.CannonRideService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Bridges Bukkit events to cannons: interaction, damage, destruction, and the flight of shots already in the air.
 */
@BPvPListener
@Singleton
@PluginAdapter("ModelEngine")
public class CannonListener implements Listener {

    @Inject
    private Core core;

    @Inject
    private CannonService cannonService;

    @Inject
    private CannonAmmoRegistry ammoRegistry;

    @Inject
    private CannonRideService rideService;

    @Inject
    private ItemFactory itemFactory;

    @Inject
    private ItemRegistry itemRegistry;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(CannonPlaceEvent event) {
        new SoundEffect(Sound.BLOCK_ANVIL_PLACE, 1f, 0.7f).play(event.getCannonLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReload(final CannonReloadEvent event) {
        final CannonProp cannon = event.getCannon();
        final Location location = cannon.getLocation();
        new SoundEffect("littleroom_cannon", "littleroom.cannon.closehatch").play(location);

        cannon.setAmmo(event.getAmmo());
        if (cannon.getCycle() != null) {
            cannon.getCycle().load();
        }
        cannonService.persist(cannon);

        final ActiveModel model = cannon.getActiveModel();
        if (model == null) {
            return;
        }
        final IPriorityHandler animations = (IPriorityHandler) model.getAnimationHandler();
        animations.playAnimation("load", 0, 0, 0.5, true);
        // Keep the barrel closed after the clip finishes
        Objects.requireNonNull(animations.getAnimation("load")).setForceLoopMode(BlueprintAnimation.LoopMode.HOLD);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCannonShoot(final CannonShootEvent event) {
        final CannonProp cannon = event.getCannon();
        final Location location = cannon.getLocation();
        new SoundEffect("littleroom_cannon", "littleroom.cannon.fire", 1f, 2f).play(location);
        new SoundEffect("littleroom_cannon", "littleroom.cannon.ringing", 1f, 1.1f).play(location);

        final ActiveModel model = cannon.getActiveModel();
        if (model != null) {
            final IPriorityHandler animations = (IPriorityHandler) model.getAnimationHandler();
            animations.playAnimation("shoot", 0, 0, 1, true);
            Objects.requireNonNull(animations.getAnimation("load")).setForceLoopMode(BlueprintAnimation.LoopMode.ONCE);
        }

        UtilServer.runTaskLater(core, () ->
                new SoundEffect("littleroom_cannon", "littleroom.cannon.openhatch").play(location), 2L);

        final Location muzzle = cannon.getMuzzle().add(cannon.getLocation().getDirection().multiply(2));
        Particle.EXPLOSION_EMITTER.builder().location(muzzle).extra(0).receivers(60).spawn();
        Particle.LARGE_SMOKE.builder().location(muzzle).extra(0).count(10).offset(1, 1, 1).receivers(60).spawn();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAim(final CannonAimEvent event) {
        event.getCannon().rotate(event.getDirection());
        new SoundEffect(Sound.BLOCK_IRON_DOOR_CLOSE, 0.3f, 0.5f).play(event.getCannon().getLocation());
    }

    /**
     * Advances every shot in the air. Collision policy comes from the projectile: the ammo's default, possibly vetoed
     * by the firing cannon.
     */
    @UpdateEvent
    public void projectileTicker() {
        final Iterator<Map.Entry<UUID, CannonProjectile>> iterator = cannonService.getProjectiles().entrySet().iterator();
        while (iterator.hasNext()) {
            final CannonProjectile projectile = iterator.next().getValue();
            final Entity entity = projectile.getEntity();
            if (!entity.isValid() || entity.isDead()) {
                iterator.remove();
                continue;
            }

            final Location last = projectile.getLastLocation();
            final Location current = entity.getLocation().clone();

            if (entity.getTicksLived() > 1 && projectile.isEntityCollision()) {
                final Optional<RayTraceResult> trace = UtilEntity.interpolateCollision(last, current, 0.8f,
                        candidate -> !candidate.equals(entity) && candidate instanceof LivingEntity
                                && !cannonService.isCannonPart(candidate));
                if (trace.isPresent()) {
                    projectile.getAmmo().onImpact(projectile, current);
                    iterator.remove();
                    continue;
                }
            }

            if (entity.getTicksLived() > 1 && projectile.isBlockCollision()) {
                final BoundingBox box = entity.getBoundingBox().clone().expand(0.1);
                if (UtilLocation.getBoundingBoxCorners(entity.getWorld(), box).stream()
                        .anyMatch(corner -> !corner.getBlock().isPassable())) {
                    projectile.getAmmo().onImpact(projectile, current);
                    iterator.remove();
                    continue;
                }
            }

            projectile.setLastLocation(current);
            projectile.getAmmo().tick(projectile);
        }
    }

    /** Credits cannon damage to whoever fired, and applies the ammo's own falloff curve. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCustomDamage(final DamageEvent event) {
        //noinspection UnstableApiUsage
        if (event.getDamageSource().getDamageType() == DamageType.PLAYER_EXPLOSION && event.getDamagingEntity() != null) {
            final CannonProjectile projectile = cannonService.projectileOf(event.getDamagingEntity());
            if (projectile != null && projectile.getShooter() != null) {
                final Player shooter = Bukkit.getPlayer(projectile.getShooter());
                if (shooter != null) {
                    event.setDamager(shooter);
                }
                final double distance = event.getDamagingEntity().getLocation().distance(event.getDamagee().getLocation());
                event.setDamage(projectile.damageAt(distance));
                event.setKnockback(false);
                event.addReason("Cannonball");
            }
        }

        cannonService.of(event.getDamagee()).ifPresent(event::setSoundProvider);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCannonDamage(final DamageEvent event) {
        if (!cannonService.isCannonPart(event.getDamagee())) {
            return;
        }

        cannonService.of(event.getDamagee()).ifPresent(cannon -> {
            if (cannon.getProperties().isInvincible()) {
                event.setCancelled(true);
            }
        });

        if (event.getBukkitCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            event.setCancelled(true);
        }
    }

    /** Persists the cannon's new health so a chunk cycle or a restart does not heal it back to full. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(final DamageEvent event) {
        if (!event.isDamageeLiving()) {
            return;
        }
        cannonService.of(event.getDamagee()).ifPresent(CannonProp::syncHealthFromEntity);
    }

    @EventHandler
    public void onInteract(final PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Player player = event.getPlayer();
        cannonService.of(event.getRightClicked()).ifPresent(cannon -> {
            if (!cannon.getProperties().isEnabled() || cannon.getCycle() == null) {
                return;
            }

            // Holding a round the cannon will take means this click is a reload, handled by the item's own ability.
            if (cannon.getArchetype().getFiringMode().consumesAmmo() && cannon.getCycleState() == CannonState.IDLE
                    && holdingAcceptedAmmo(player, cannon)) {
                return;
            }

            if (cannon.getCycleState().isBusy()) {
                return; // mid-sequence: no aiming, no re-triggering
            }

            if (player.isSneaking()) {
                if (!triggerChecks(player, cannon)) {
                    return;
                }
                if (cannon.getArchetype().getFiringMode().onTriggerRequested(cannon, player)) {
                    event.setCancelled(true);
                }
                return;
            }

            if (cannon.getArchetype().getFiringMode().onInteract(cannon, player)) {
                event.setCancelled(true);
                return;
            }

            if (!cannon.getProperties().isAllowRotation()) {
                return;
            }

            final Vector current = cannon.getLocation().getDirection();
            final Vector target = player.getLocation().getDirection();
            final CannonAimEvent aimEvent = new CannonAimEvent(cannon, player, UtilMath.rotateTo(current, target, 0.2f));
            aimEvent.callEvent();
            if (!aimEvent.isCancelled()) {
                event.setCancelled(true);
            }
        });
    }

    /** Shared refusals for "make this cannon go off", with the feedback message. */
    private boolean triggerChecks(@NotNull Player player, @NotNull CannonProp cannon) {
        if (cannon.getCycle() != null && !cannon.getCycle().isReady()) {
            UtilMessage.message(player, "core.prefix.combat", "core.cannon.cooling_down");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return false;
        }

        if (cannon.getArchetype().getFiringMode().consumesAmmo() && cannon.getAmmo() == null) {
            UtilMessage.message(player, "core.prefix.combat", "core.cannon.not_loaded",
                    Translations.component("core.item.cannonball.name").color(NamedTextColor.YELLOW));
            SoundEffect.LOW_PITCH_PLING.play(player);
            return false;
        }
        return true;
    }

    private boolean holdingAcceptedAmmo(@NotNull Player player, @NotNull CannonProp cannon) {
        return heldAmmo(player, EquipmentSlot.HAND).or(() -> heldAmmo(player, EquipmentSlot.OFF_HAND))
                .filter(ammo -> cannon.getArchetype().accepts(ammo.id()))
                .isPresent();
    }

    private Optional<CannonAmmo> heldAmmo(@NotNull Player player, @NotNull EquipmentSlot slot) {
        final ItemStack stack = slot == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        final Optional<ItemInstance> instance = itemFactory.fromItemStack(stack);
        return instance.flatMap(ammoRegistry::byItem);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(final EntityDeathEvent event) {
        cannonService.of(event.getEntity()).ifPresent(cannon -> {
            event.setDroppedExp(0);
            event.getDrops().clear();

            if (cannon.getProperties().isInvincible()) {
                event.setCancelled(true);
                return;
            }

            destroy(cannon);

            // Give back whatever was chambered
            final CannonAmmo ammo = cannon.getAmmo();
            if (ammo != null) {
                final BaseItem item = itemRegistry.getItem(ammo.itemKey());
                if (item != null) {
                    final Location location = cannon.getLocation();
                    location.getWorld().dropItemNaturally(location, itemFactory.create(item).createItemStack());
                }
            }
        });
    }

    /**
     * Destroys a cannon whose body was removed by something other than the scene framework. A framework
     * dematerialization despawns the golem with the same {@code DISCARDED} reason as a genuine removal, so the cannon's
     * own {@link CannonProp#isDematerializing()} flag is what separates a chunk unload from a destruction.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(final EntityRemoveFromWorldEvent event) {
        if (!UtilEntity.isRemoved(event.getEntity()) || !UtilEntity.getRemovalReason(event.getEntity()).isDestroy()) {
            return;
        }
        cannonService.of(event.getEntity())
                .filter(cannon -> !cannon.isDematerializing())
                .ifPresent(this::destroy);
    }

    /** Common teardown: evict any rider, remove the cannon and its record, and play the wreck effect. */
    private void destroy(@NotNull CannonProp cannon) {
        rideService.evictFrom(cannon);
        final Location location = cannon.getLocation();
        cannonService.remove(cannon);
        Particle.LARGE_SMOKE.builder().location(location).extra(0).count(10).offset(1, 1, 1).receivers(60).spawn();
    }

    @EventHandler
    public void onEntityVelocity(final CustomEntityVelocityEvent event) {
        if (cannonService.isCannonPart(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(final EntityTargetEvent event) {
        if (event.getTarget() != null && cannonService.isCannonPart(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplodeBlocks(EntityExplodeEvent event) {
        final CannonProjectile projectile = cannonService.projectileOf(event.getEntity());
        if (projectile == null) {
            return;
        }

        final UUID ownerUuid = event.getEntity().getPersistentDataContainer()
                .get(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID);
        final Player player = ownerUuid == null ? null : Bukkit.getPlayer(ownerUuid);
        final CannonballExplodeEvent explodeEvent = new CannonballExplodeEvent(null, projectile,
                event.getEntity().getLocation(), player);
        explodeEvent.callEvent();

        if (explodeEvent.isCancelled() || !projectile.isBreaksBlocks()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(EntityMoveEvent event) {
        if (!cannonService.isCannonPart(event.getEntity())) {
            return;
        }
        cannonService.of(event.getEntity())
                .filter(cannon -> !cannon.getProperties().isMovable())
                .ifPresent(cannon -> event.setCancelled(true));
    }
}
