package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.StampedeData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@CustomLog
public class Stampede extends Skill implements PassiveSkill, MovementSkill, DamageSkill {

    private final WeakHashMap<Player, StampedeData> playerData = new WeakHashMap<>();
    private double durationPerStack;
    private double damage;
    private int maxSpeedStrength;
    private double durationPerStackDecreasePerLevel;
    private double damageIncreasePerLevel;
    private double knockback;
    private double knockbackIncreasePerLevel;

    @Inject
    public Stampede(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Stampede";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You slowly build up speed as you",
                "sprint, gaining one level of <effect>Speed</effect>",
                "for every " + getValueString(this::getDurationPerStack, level) + " seconds, up to a max",
                "of <effect>Speed " + UtilFormat.getRomanNumeral(maxSpeedStrength) + "</effect>",
                "",
                "Attacking during stampede deals " + getValueString(this::getDamage, level) + " bonus",
                "bonus damage and <val>" + getValueString(this::getBonusKnockback, level, 2) + "x</val> extra knockback",
                "per speed level"
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        playerData.remove(player);
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getBonusKnockback(int level) {
        return knockback + ((level - 1) * knockbackIncreasePerLevel);
    }

    public double getDurationPerStack(int level) {
        return durationPerStack - ((level - 1) * durationPerStackDecreasePerLevel);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @UpdateEvent(delay = 200)
    public void updateSpeed() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(player.isSprinting() && !playerData.containsKey(player)) {
                if(getLevel(player) > 0) {
                    startStampede(player);
                }
            }
        }

        Iterator<Map.Entry<Player, StampedeData>> iterator = playerData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, StampedeData> entry = iterator.next();
            Player player = entry.getKey();
            StampedeData data = entry.getValue();
            int level = getLevel(player);

            if (level < 1) {
                removeSpeed(player);
                iterator.remove();
                continue;
            }

            boolean isSprintingNow = player.isSprinting() && !player.isInWater();

            if (data == null) {
                data = new StampedeData(System.currentTimeMillis(), 0);
                playerData.put(player, data);
            } else if (isSprintingNow) {
                if (UtilTime.elapsed(data.getSprintTime(), (long) ((getDurationPerStack(level)) * 1000L))) {
                    if (data.getSprintStrength() < maxSpeedStrength) {
                        data.setSprintTime(System.currentTimeMillis());
                        data.setSprintStrength(data.getSprintStrength() + 1);

                        championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, getName(), data.getSprintStrength(), 2200, true);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2.0F, 0.2F * data.getSprintStrength() + 1.2F);
                        UtilMessage.simpleMessage(player, getClassType().getName(), "Stampede Level: <yellow>%d", data.getSprintStrength());
                    }
                }
                if (data.getSprintStrength() > 0) {
                    championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, getName(), data.getSprintStrength(), 2200, true);
                }
            } else {
                removeSpeed(player);
                iterator.remove();
            }
        }

    }

    public void removeSpeed(Player player) {
        championsManager.getEffects().removeEffect(player, EffectTypes.SPEED, getName());
    }

    private void startStampede(Player player) {
        StampedeData data = playerData.getOrDefault(player, new StampedeData(System.currentTimeMillis(), 0));
        data.setSprintTime(System.currentTimeMillis());
        playerData.put(player, data);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        playerData.remove(damagee);
        removeSpeed(damagee);
        startStampede(damagee);
    }


    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        StampedeData data = playerData.get(damager);
        if (data == null || data.getSprintStrength() < 1) return;

        int str = data.getSprintStrength();
        int level = getLevel(damager);

        event.setKnockback(false);
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 1.5F);
        VelocityData velocityData = new VelocityData(UtilVelocity.getTrajectory2d(damager, event.getDamagee()), getBonusKnockback(level) * str, true, 0.0D, 0.4D, 1.0D, true);
        UtilVelocity.velocity(event.getDamagee(), damager, velocityData, VelocityType.KNOCKBACK);

        double additionalDamage = getDamage(level) * str;
        event.setDamage(event.getDamage() + additionalDamage);

        playerData.remove(damager);
        removeSpeed(damager);
        startStampede(damager);
    }


    @Override
    public void loadSkillConfig() {
        durationPerStack = getConfig("durationPerStack", 6.0, Double.class);
        durationPerStackDecreasePerLevel = getConfig("durationPerStackDecreasePerLevel", 1.0, Double.class);
        damage = getConfig("damage", 0.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        maxSpeedStrength = getConfig("maxSpeedStrength", 3, Integer.class);
        knockbackIncreasePerLevel = getConfig("knockbackIncreasePerLevel", 0.5, Double.class);
        knockback = getConfig("knockback", 0.5, Double.class);
    }
}
