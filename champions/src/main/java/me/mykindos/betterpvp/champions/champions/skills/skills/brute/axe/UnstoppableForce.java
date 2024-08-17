package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
@Singleton
public class UnstoppableForce extends ChannelSkill implements InteractSkill, EnergyChannelSkill, CrowdControlSkill, MovementSkill, OffensiveSkill {

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double hitboxExpansion;
    private double chargeSensitivity;
    private double knockbackStrength;
    private HashMap<UUID, Vector> initialDirections = new HashMap<>();

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
                "Hold right click with a Sword to channel",
                "",
                "Raise your shield and begin to charge at high speed.",
                "Deals <val>" + getDamage(level) + "</val> damage and knocks back any enemy hit.",
                "",
                "Energy: <val>" + getEnergy(level) + "</val> per second",
                "Cooldown: <val>" + getCooldown(level) + "</val> seconds starting when the charge ends"
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
        initialDirections.put(player.getUniqueId(), player.getLocation().getDirection());
    }

    @UpdateEvent
    public void doCharge() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                iterator.remove();
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                finishCharge(player);
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                finishCharge(player);
                iterator.remove();
                continue;
            }

            if (!UtilBlock.isGrounded(player)) {
                continue;
            }

            if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
                finishCharge(player);
                iterator.remove();
                continue;
            }

            if (!isHolding(player)) {
                finishCharge(player);
                iterator.remove();
                continue;
            }

            championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, getName(), 1, 100, true);

            final Location newLocation = UtilPlayer.getMidpoint(player).clone();

            Vector initialDirection = initialDirections.get(player.getUniqueId());
            if (initialDirection == null) {
                finishCharge(player);
                iterator.remove();
                continue;
            }

            Vector currentDirection = player.getLocation().getDirection();
            Vector newDirection = initialDirection.clone().multiply(1 - chargeSensitivity).add(currentDirection.clone().multiply(chargeSensitivity)).normalize();
            initialDirections.put(player.getUniqueId(), newDirection);

            double slownessMultiplier = 1.0;
            if (player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                int slownessLevel = player.getPotionEffect(PotionEffectType.SLOWNESS).getAmplifier() + 1;
                slownessMultiplier = Math.max(0, 1 - (slownessLevel * 0.15));
            }

            VelocityData velocityData = new VelocityData(newDirection, 0.6 * slownessMultiplier, true, 0, 0, 0.0, false);
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
                    VelocityData targetVelocityData = new VelocityData(newDirection, knockbackStrength, true, 0.4, 0.4, 0.4, true);
                    UtilVelocity.velocity(target, player, targetVelocityData, VelocityType.KNOCKBACK_CUSTOM);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 0.9f);
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

    private void finishCharge(Player player) {
        championsManager.getCooldowns().use(player, getName(), getCooldown(getLevel(player)), true,
                true, false, isHolding(player) && (getType() == SkillType.AXE));
        championsManager.getEffects().removeEffect(player, EffectTypes.NO_JUMP, getName());
        initialDirections.remove(player.getUniqueId());
    }

    @EventHandler
    public void onNegativeEffect(EffectReceiveEvent event) {
        if (!active.contains(event.getTarget().getUniqueId())) return;
        if (event.getEffect().getEffectType() == EffectTypes.SILENCE) {
            finishCharge((Player)event.getTarget());
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
        if (!UtilBlock.isGrounded(player) && isHolding(player)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You can only use <alt>" + getName() + "</alt> while grounded.");
            return false;
        }

        return true;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        cooldown = getConfig("cooldown", 15.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
        energy = getConfig("energy", 70, Integer.class);
        energyDecreasePerLevel = getConfig("energyDecreasePerLevel", 10.0, Double.class);
        hitboxExpansion = getConfig("hitboxExpansion", 1.2, Double.class);
        chargeSensitivity = getConfig("chargeSensitivity", 0.01, Double.class);
        knockbackStrength = getConfig("knockbackStrength", 1.5, Double.class);
    }
}