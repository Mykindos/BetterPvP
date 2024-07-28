package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class BloodBarrier extends Skill implements InteractSkill, CooldownSkill, Listener, HealthSkill, TeamSkill, BuffSkill, DefensiveSkill {

    private final HashMap<UUID, ShieldData> shieldDataMap = new HashMap<>();

    private double baseDuration;

    private double durationIncreasePerLevel;
    private double baseRange;
    private double rangeIncreasePerLevel;
    private double baseDamageReduction;
    private double damageReductionPerLevel;
    private int baseNumAttacksToReduce;
    private int numAttacksToReducePerLevel;

    private double baseHealthReduction;
    private double healthReductionDecreasePerLevel;
    private double baseHealthReductionPerPlayerAffected;
    private double healthReductionPerPlayerAffectedDecreasePerLevel;

    @Inject
    public BloodBarrier(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Blood Barrier";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Grant yourself and allies within " + getValueString(this::getRange, level) + " blocks",
                "a barrier which reduces the damage of the next " + getValueString(this::numAttacksToReduce, level, 0),
                "incoming attacks by " + getValueString(this::getDamageReduction, level, 100, "%", 0),
                "",
                "Barrier lasts for " + getValueString(this::getDuration, level) + " seconds, and does not stack",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "Health Sacrifice: " + getValueString(this::getHealthReduction, level, 1) + " + " + getValueString(this::getHealthReductionPerPlayerAffected, level, 1) + " per player affected",
        };
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - ((level - 1) * healthReductionDecreasePerLevel);
    }

    public double getHealthReductionPerPlayerAffected(int level) {
        return baseHealthReductionPerPlayerAffected - ((level - 1) * healthReductionPerPlayerAffectedDecreasePerLevel);
    }

    public int numAttacksToReduce(int level) {
        return baseNumAttacksToReduce + ((level - 1) * numAttacksToReducePerLevel);
    }

    public double getRange(int level) {
        return baseRange + ((level - 1) * rangeIncreasePerLevel);
    }

    public double getDamageReduction(int level) {
        return baseDamageReduction + ((level - 1) * damageReductionPerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }


    @EventHandler
    public void removeOnDeath(PlayerDeathEvent event) {
        shieldDataMap.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (event.getDamagee() instanceof Player player) {

            ShieldData shieldData = shieldDataMap.get(player.getUniqueId());
            if (shieldData != null) {
                event.setDamage(event.getDamage() * (1 - shieldData.getDamageReduction()));
                shieldData.count--;
            }
        }
    }

    @UpdateEvent
    public void update() {
        if (shieldDataMap.isEmpty()) return;
        shieldDataMap.entrySet().removeIf(entry -> {
            if (entry.getValue().count <= 0) {
                return true;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) return true;

            if (entry.getValue().getEndTime() - System.currentTimeMillis() <= 0) {
                UtilMessage.message(player, getClassType().getName(), "Your blood barrier has expired.");
                return true;
            }

            boolean hasRole = championsManager.getRoles().hasRole(player);
            if((entry.getValue().hasRole && !hasRole) || (!entry.getValue().hasRole && hasRole)) {
                return true;
            }

            return false;
        });

        shieldDataMap.forEach((key, value) -> {
            Player player = Bukkit.getPlayer(key);
            if (player != null) {

                double oX = Math.sin(player.getTicksLived() / 10d);
                double oZ = Math.cos(player.getTicksLived() / 10d);
                Particle.DUST.builder().location(player.getLocation().add(oX, 0.7, oZ)).extra(0).color(200, 0, 0).receivers(30).spawn();
            }
        });

    }


    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);

        if (player.getHealth() - getHealthReduction(level) <= 1) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName(), level);
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int level) {
        double healthReduction = getHealthReduction(level);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 2.0f, 1.0f);

        boolean playerHasRole = championsManager.getRoles().hasRole(player);
        shieldDataMap.put(player.getUniqueId(), new ShieldData((long) (getDuration(level) * 1000), numAttacksToReduce(level), getDamageReduction(level), playerHasRole));
        for (Player ally : UtilPlayer.getNearbyAllies(player, player.getLocation(), getRange(level))) {

            if(player.getHealth() - (healthReduction + getHealthReductionPerPlayerAffected(level)) < 1) {
                break;
            }

            boolean allyHasRole = championsManager.getRoles().hasRole(ally);
            shieldDataMap.put(ally.getUniqueId(), new ShieldData((long) (getDuration(level) * 1000), numAttacksToReduce(level), getDamageReduction(level), allyHasRole));
            healthReduction += getHealthReductionPerPlayerAffected(level);
        }

        UtilPlayer.slowHealth(champions, player, -healthReduction, 5, false);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseRange = getConfig("baseRange", 8.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 1.0, Double.class);

        baseHealthReduction = getConfig("baseHealthReduction", 6.0, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.50, Double.class);

        baseHealthReductionPerPlayerAffected = getConfig("baseHealthReductionPerPlayerAffected", 1.0, Double.class);
        healthReductionPerPlayerAffectedDecreasePerLevel = getConfig("healthReductionPerPlayerAffectedDecreasePerLevel", 0.0, Double.class);

        baseDuration = getConfig("baseDuration", 20.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 2.5, Double.class);

        baseDamageReduction = getConfig("damageReduction", 0.30, Double.class);
        damageReductionPerLevel = getConfig("damageReductionPerLevel", 0.0, Double.class);

        baseNumAttacksToReduce = getConfig("baseNumAttacksToReduce", 3, Integer.class);
        numAttacksToReducePerLevel = getConfig("numAttacksToReducePerLevel", 0, Integer.class);
    }

    @Data
    private static class ShieldData {

        private final long endTime;

        public int count;

        public double damageReduction;

        public boolean hasRole;

        public ShieldData(long length, int count, double damageReduction, boolean hasRole) {
            this.endTime = System.currentTimeMillis() + length;
            this.count = count;
            this.damageReduction = damageReduction;
            this.hasRole = hasRole;
        }

    }
}

