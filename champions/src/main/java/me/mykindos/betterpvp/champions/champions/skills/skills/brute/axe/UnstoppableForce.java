package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
@Singleton
public class UnstoppableForce extends ChannelSkill implements InteractSkill, EnergyChannelSkill, CrowdControlSkill, MovementSkill, OffensiveSkill {

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double hitboxExpansion;

    @Inject
    public UnstoppableForce(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Unstoppable Force";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with an axe to channel",
                "",
                "Raise your shield and begin to charge at high speed.",
                "Deals " + getValueString(this::getDamage, level) + " damage and knocks back any enemy hit.",
                "",
                "While charging, you are immune to any crowd control effects",
                "",
                "Energy: " + getValueString(this::getEnergy, level) + " per second",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds starting when the charge ends"

        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        if (championsManager.getCooldowns().hasCooldown(player, getName())) {
            UtilMessage.simpleMessage(player, "Cooldown", "You cannot use <alt>%s %s</alt> for <alt>%s</alt> seconds.", getName(), level,
                    Math.max(0, championsManager.getCooldowns().getAbilityRecharge(player, getName()).getRemaining()));
            return;
        }
        active.add(player.getUniqueId());
    }

    @UpdateEvent
    public void doUnstoppableForce() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }


            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                finishUnstoppableForce(player);
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                finishUnstoppableForce(player);
                iterator.remove();
            } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
                finishUnstoppableForce(player);
                iterator.remove();
            } else if (!isHolding(player)) {
                finishUnstoppableForce(player);
                iterator.remove();

            }

            if (UtilBlock.isGrounded(player)) {
                championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, getName(), 1, 100, true);

                final Location newLocation = UtilPlayer.getMidpoint(player).clone();

                VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), 0.5, true, 0, 0, 0.0, false);
                UtilVelocity.velocity(player, null, velocityData);

                final Optional<LivingEntity> hit = UtilEntity.interpolateCollision(newLocation,
                                newLocation.clone().add(velocityData.getVector()),
                                (float) hitboxExpansion,
                                ent -> UtilEntity.IS_ENEMY.test(player, ent))
                        .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

                if (hit.isPresent()) {
                    final LivingEntity target = hit.get();
                    var cde = UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, player, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, getName()));
                    if (cde != null && !cde.isCancelled()) {
                        VelocityData targetVelocityData = new VelocityData(player.getLocation().getDirection(), 2, true, 0.4, 0.4, 0.4, true);
                        UtilVelocity.velocity(target, player, targetVelocityData, VelocityType.KNOCKBACK_CUSTOM);
                        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 0.9f);
                    }
                }
            }
        }

    }

    @UpdateEvent(delay = 100)
    public void doSound() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && UtilBlock.isGrounded(player)) {
                UtilSound.playSound(player.getWorld(), player, Sound.ENTITY_PLAYER_SMALL_FALL, 0.5f, 0.7f);
            }
        }
    }

    private void finishUnstoppableForce(Player player) {
        championsManager.getCooldowns().use(player, getName(), getCooldown(getLevel(player)), true,
                true, false, isHolding(player) && (getType() == SkillType.AXE));
        championsManager.getEffects().removeEffect(player, EffectTypes.NO_JUMP, getName());
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!active.contains(event.getEntity().getUniqueId())) return;

        var effect = event.getNewEffect();
        if (effect == null) return;

        if (effect.getType() == PotionEffectType.SLOW || effect.getType() == PotionEffectType.LEVITATION) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onNegativeEffect(EffectReceiveEvent event) {
        if (!active.contains(event.getTarget().getUniqueId())) return;
        if (event.getEffect().getEffectType() == EffectTypes.STUN
                || event.getEffect().getEffectType() == EffectTypes.SILENCE
                || event.getEffect().getEffectType() == EffectTypes.SLOWNESS) {
            event.setCancelled(true);
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public boolean isShieldInvisible() {
        return false;
    }

    @Override
    public boolean shouldShowShield(Player player) {
        return !championsManager.getCooldowns().hasCooldown(player, getName());
    }

    @Override
    public boolean canUse(Player player) {
        return isHolding(player);
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        cooldown = getConfig("cooldown", 15.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
        energy = getConfig("energy", 55, Integer.class);
        energyDecreasePerLevel = getConfig("energyDecreasePerLevel", 1.0, Double.class);
        hitboxExpansion = getConfig("hitboxExpansion", 0.3, Double.class);
    }
}
