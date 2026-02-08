package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageCause;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import static me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory.RANGED;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class WindSlashAbility extends CooldownInteraction implements DisplayedInteraction {

    private double slashCooldown;
    private double slashHitboxSize;
    private int slashEnergyCost;
    private double slashDamage;
    private double slashEnergyRefundPercent;
    private double slashVelocity;
    private int slashAliveMillis;
    private double slashSpeed;

    @EqualsAndHashCode.Exclude
    private final EnergyService energyService;
    @EqualsAndHashCode.Exclude
    private final BaseItem heldItem;

    // Active slashes
    @EqualsAndHashCode.Exclude
    private final Set<Slash> slashSet = new HashSet<>();

    public WindSlashAbility(CooldownManager cooldownManager, EnergyService energyService, BaseItem heldItem) {
        super("wind_slash", cooldownManager);
        this.heldItem = heldItem;
        this.energyService = energyService;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Wind Slash");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Shoot out 3 wind bursts. When they land on an enemy, recover some energy and deal damage to them.");
    }

    @Override
    public double getCooldown() {
        return slashCooldown;
    }

    @Override
    public double getEnergyCost() {
        return slashEnergyCost;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Consume durability
        if (itemStack != null) {
            UtilItem.damageItem(player, itemStack, 1);
        }

        // SFX
        new SoundEffect(Sound.ENTITY_PHANTOM_FLAP, 1.2F, 2.0F).play(player.getLocation());

        // Fire three slashes
        Location origin = player.getEyeLocation();
        final double rotation = Math.toRadians(30);

        Vector mainDirection = origin.getDirection().normalize();
        Vector leftDirection = mainDirection.clone().rotateAroundY(rotation).normalize();
        Vector rightDirection = mainDirection.clone().rotateAroundY(-rotation).normalize();

        Slash mainSlash = new Slash(player, origin);
        mainSlash.redirect(mainDirection.multiply(slashSpeed));
        Slash leftSlash = new Slash(player, origin);
        leftSlash.redirect(leftDirection.multiply(slashSpeed));
        Slash rightSlash = new Slash(player, origin);
        rightSlash.redirect(rightDirection.multiply(slashSpeed));

        slashSet.add(mainSlash);
        slashSet.add(leftSlash);
        slashSet.add(rightSlash);
        return InteractionResult.Success.ADVANCE;
    }

    // Call this from a scheduler in the main item class
    public void processSlashes() {
        final Iterator<Slash> slashIterator = slashSet.iterator();
        while (slashIterator.hasNext()) {
            final Slash slash = slashIterator.next();
            final Player player = slash.getCaster();

            if (!player.isOnline() || slash.isMarkForRemoval() || slash.isExpired()) {
                slashIterator.remove(); // Remove offline players or done lines
                continue;
            }

            slash.tick();
        }
    }

    private class Slash extends Projectile {

        private final Set<LivingEntity> hitTargets = new HashSet<>();

        private Slash(@Nullable Player caster, Location location) {
            super(caster, slashHitboxSize, location, slashAliveMillis);
        }

        @Override
        protected void onTick() {
            final Collection<Player> receivers = location.getNearbyPlayers(60);
            for (Location point : interpolateLine()) {
                // Play travel particles
                Particle.POOF.builder()
                        .location(point)
                        .offset(0, 0, 0)
                        .count(1)
                        .receivers(receivers)
                        .extra(0)
                        .spawn();
            }
        }

        @Override
        protected boolean canCollideWith(Entity entity) {
            if (!super.canCollideWith(entity) || hitTargets.contains(entity)) {
                return false;
            }

            final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(caster, (LivingEntity) entity);
            event.callEvent();
            return event.getResult() != Event.Result.DENY;
        }

        @Override
        protected void onImpact(Location location, RayTraceResult result) {
            if (result.getHitBlock() != null) {
                markForRemoval = true;
                return;
            }

            final LivingEntity target = (LivingEntity) Objects.requireNonNull(result.getHitEntity());
            hitTargets.add(target);

            DamageEvent event = new DamageEvent(target,
                    caster,
                    null,
                    new InteractionDamageCause(WindSlashAbility.this).withCategory(RANGED),
                    slashDamage,
                    "Wind Slash");
            UtilDamage.doDamage(event);

            if (event.isCancelled() || caster == null || !caster.isOnline()) {
                return; // Cancelled or offline, dont do anything
            }

            // Knockback
            final Vector direction = this.velocity.clone().normalize();
            UtilVelocity.velocity(target, caster, new VelocityData(
                    direction.clone(),
                    slashVelocity,
                    0,
                    slashVelocity,
                    true
            ));

            // SFX
            new SoundEffect(Sound.ENTITY_PUFFER_FISH_STING, 0.8F, 1.5F).play(target.getLocation());

            // Regen energy
            energyService.regenerateEnergy(caster, slashEnergyRefundPercent, EnergyEvent.Cause.USE);
        }
    }
}
