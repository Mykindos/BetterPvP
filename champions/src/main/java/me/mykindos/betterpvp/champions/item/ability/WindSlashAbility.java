package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class WindSlashAbility extends ItemAbility {

    private double slashCooldown;
    private double slashHitboxSize;
    private int slashEnergyCost;
    private double slashDamage;
    private double slashEnergyRefundPercent;
    private double slashVelocity;
    private int slashAliveMillis;
    private double slashSpeed;

    @EqualsAndHashCode.Exclude
    private final EnergyHandler energyHandler;
    @EqualsAndHashCode.Exclude
    private final CooldownManager cooldownManager;

    // Active slashes
    @EqualsAndHashCode.Exclude
    private final Set<Slash> slashSet = new HashSet<>();

    public WindSlashAbility(CooldownManager cooldownManager, EnergyHandler energyHandler) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "wind_slash"),
                "Wind Slash",
                "Shoot out 3 wind bursts. When they land on an enemy, recover some energy and deal damage to them.",
                TriggerType.LEFT_CLICK);
        this.cooldownManager = cooldownManager;
        this.energyHandler = energyHandler;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!cooldownManager.use(player, getName(), slashCooldown, true, true, false)) {
            return false;
        }

        if (!energyHandler.use(player, getName(), slashEnergyCost, true)) {
            return false;
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
        return true;
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

            CustomDamageEvent event = new CustomDamageEvent(target,
                    caster,
                    null,
                    EntityDamageEvent.DamageCause.CUSTOM,
                    slashDamage,
                    false,
                    "Wind Slash");
            UtilDamage.doCustomDamage(event);

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
            energyHandler.regenerateEnergy(caster, slashEnergyRefundPercent);
        }
    }
} 