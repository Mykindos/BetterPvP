package me.mykindos.betterpvp.champions.item.thornfang;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageModifier;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Needlegrasp extends CooldownInteraction implements Listener, DisplayedInteraction {

    private double cooldown;
    private double damage;
    private double hitboxSize;
    private double speed;
    private double airDuration;
    private double pullDuration;
    private double pullSpeed;
    private double recoilStrength;
    private double gracePeriodSeconds;

    private transient final CooldownManager cooldownManager;
    private transient final Champions champions;
    private transient final HuntersBrand huntersBrand;
    private transient final WeakHashMap<Player, NeedlegraspProjectile> activeProjectiles = new WeakHashMap<>();
    private transient final WeakHashMap<Player, PullState> activePulls = new WeakHashMap<>();

    protected Needlegrasp(Champions champions, CooldownManager cooldownManager, HuntersBrand huntersBrand) {
        super("needlegrasp", cooldownManager);
        this.champions = champions;
        this.cooldownManager = cooldownManager;
        this.huntersBrand = huntersBrand;
        Bukkit.getPluginManager().registerEvents(this, champions);
        UtilServer.runTaskTimer(champions, this::tick, 0, 1);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Needlegrasp");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Throw a vine that pulls you and an enemy toward each other. Hit them with a melee attack to reset your cooldown.");
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Play throw sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0F, 1.2F);

        // Remove existing projectile if any
        NeedlegraspProjectile existing = activeProjectiles.remove(player);
        if (existing != null) {
            existing.remove();
        }

        // Create and launch new projectile
        Location eyeLocation = player.getEyeLocation();
        NeedlegraspProjectile projectile = new NeedlegraspProjectile(
                player,
                this,
                damage,
                hitboxSize,
                eyeLocation,
                (long) (airDuration * 1000),
                (long) (pullDuration * 1000),
                pullSpeed
        );
        projectile.redirect(player.getLocation().getDirection().multiply(speed));

        activeProjectiles.put(player, projectile);
        return InteractionResult.Success.ADVANCE;
    }

    public void tick() {
        // Clean up projectiles
        Iterator<Map.Entry<Player, NeedlegraspProjectile>> projIterator = activeProjectiles.entrySet().iterator();

        while (projIterator.hasNext()) {
            Map.Entry<Player, NeedlegraspProjectile> entry = projIterator.next();
            Player player = entry.getKey();
            NeedlegraspProjectile projectile = entry.getValue();

            if (projectile == null) {
                projIterator.remove();
                continue;
            }

            // Remove if player is offline or projectile is expired
            if (player == null || !player.isValid() || player.isDead() || projectile.isMarkForRemoval() || projectile.isExpired()) {
                projectile.remove();
                projIterator.remove();
                continue;
            }

            projectile.tick();
        }

        // Clean up expired grace periods
        Iterator<Map.Entry<Player, PullState>> pullIterator = activePulls.entrySet().iterator();
        long gracePeriodMillis = (long) (gracePeriodSeconds * 1000);

        while (pullIterator.hasNext()) {
            Map.Entry<Player, PullState> entry = pullIterator.next();
            final Player player = entry.getKey();
            if (player == null || !player.isValid()) {
                pullIterator.remove();
                continue;
            }

            PullState state = entry.getValue();

            // Remove if effects applied or grace period expired
            if (state.isEffectsApplied() || (state.isCollisionOccurred() && !state.isInGracePeriod(gracePeriodMillis))) {
                pullIterator.remove();
            }
        }
    }

    /**
     * Resets the cooldown for this ability for the given player.
     * Called when the player successfully hits an enemy mid-air.
     */
    public void resetCooldown(Player player) {
        cooldownManager.removeCooldown(player, "Needlegrasp", false);
    }

    /**
     * Called by projectile when collision occurs (players get within 2 blocks).
     * Marks the collision and starts the grace period.
     */
    public void markCollisionOccurred(Player caster, LivingEntity target) {
        PullState state = activePulls.computeIfAbsent(caster, k -> new PullState(target.getUniqueId()));
        state.markCollisionOccurred();

        caster.setVelocity(new Vector());
        target.setVelocity(new Vector());

        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 2f, 1f).play(caster.getLocation());

        Particle.BLOCK.builder()
                .data(Material.EMERALD_BLOCK.createBlockData())
                .count(100)
                .offset(0.5, 0.5, 0.5)
                .location(caster.getLocation())
                .receivers(30)
                .spawn();
    }

    /**
     * Triggers the collision effects: damage, recoil, and cooldown reset.
     * Called when player lands a melee hit during the grace period.
     */
    private void triggerCollision(Player caster, LivingEntity target) {
        PullState state = activePulls.get(caster);
        if (state == null || state.isEffectsApplied()) {
            return;
        }

        // Apply recoil to target
        VelocityData recoilVelocityTarget = new VelocityData(
                target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize(),
                recoilStrength,
                false,
                0,
                1.5,
                1.0,
                true
        );
        UtilVelocity.velocity(target, caster, recoilVelocityTarget);

        // Apply opposite recoil to caster
        VelocityData recoilVelocityCaster = new VelocityData(
                caster.getLocation().toVector().subtract(target.getLocation().toVector()).normalize(),
                recoilStrength,
                false,
                0,
                1.5,
                1.0,
                true
        );
        UtilVelocity.velocity(caster, caster, recoilVelocityCaster);


        // Reset cooldown
        resetCooldown(caster);

        // Notify Hunter's Brand about the reset
        if (huntersBrand != null) {
            huntersBrand.onNeedlegraspReset(caster, target);
        }

        // Mark effects as applied
        state.setEffectsApplied(true);

        // Remove projectile if still active
        NeedlegraspProjectile projectile = activeProjectiles.get(caster);
        if (projectile != null) {
            projectile.setMarkForRemoval(true);
        }
    }

    /**
     * Event handler for melee attacks.
     * Triggers collision effects if player hits the target during grace period after collision.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMeleeAttack(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player caster)) return;
        if (!(event.getDamagee() instanceof LivingEntity target)) return;

        PullState state = activePulls.get(caster);
        if (state == null) return;

        // Check if target matches
        if (!state.getTargetUuid().equals(target.getUniqueId())) return;

        // Check if collision occurred
        if (!state.isCollisionOccurred()) return;

        // Check if within grace period
        long gracePeriodMillis = (long)(gracePeriodSeconds * 1000);
        if (!state.isInGracePeriod(gracePeriodMillis)) {
            activePulls.remove(caster);
            return;
        }

        // Check if effects already applied
        if (state.isEffectsApplied()) return;

        // Cancel the melee damage
        event.addModifier(new InteractionDamageModifier.Flat(this, damage));
        event.setKnockback(false);

        // Trigger collision effects
        triggerCollision(caster, target);
    }

    @Data
    private static class PullState {
        private final UUID targetUuid;
        private long collisionTime;  // 0 if collision hasn't occurred yet
        private boolean effectsApplied;

        public PullState(UUID targetUuid) {
            this.targetUuid = targetUuid;
            this.collisionTime = 0;
            this.effectsApplied = false;
        }

        public void markCollisionOccurred() {
            if (collisionTime == 0) {
                collisionTime = System.currentTimeMillis();
            }
        }

        public boolean isCollisionOccurred() {
            return collisionTime > 0;
        }

        public boolean isInGracePeriod(long gracePeriodMillis) {
            // If no collision yet, not in grace period
            if (collisionTime == 0) return false;
            // Check if within grace period
            return !UtilTime.elapsed(collisionTime, gracePeriodMillis);
        }
    }
}
