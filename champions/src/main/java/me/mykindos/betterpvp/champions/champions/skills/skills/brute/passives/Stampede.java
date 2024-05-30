package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.StampedeData;
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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Stampede extends Skill implements PassiveSkill {

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
                "for every <val>" + getDurationPerStack(level) + "</val> seconds, up to a max",
                "of <effect>Speed " + UtilFormat.getRomanNumeral(maxSpeedStrength) + "</effect>",
                "",
                "Attacking during stampede deals <val>" + getDamage(level) + "</val> bonus",
                "bonus damage and <val>" + getBonusKnockback(level) + "x</val> extra knockback",
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
        for (Map.Entry<Player, StampedeData> entry : playerData.entrySet()) {
            Player player = entry.getKey();
            StampedeData data = entry.getValue();
            int level = getLevel(player);
            if (level < 1) return;

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
            }
        }
    }

    public void removeSpeed(Player player) {
        StampedeData data = playerData.get(player);
        if (data == null || data.getSprintStrength() < 1) return;

        playerData.remove(player);

        championsManager.getEffects().removeEffect(player, EffectTypes.SPEED, getName());
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();

        if (event.isSprinting()) {
            if (getLevel(player) > 0) {
                startStampede(player);
            }
        } else {
            removeSpeed(player);
        }
    }

    private void startStampede(Player player) {
        StampedeData data = playerData.getOrDefault(player, new StampedeData(System.currentTimeMillis(), 0));
        data.setSprintTime(System.currentTimeMillis());
        playerData.put(player, data);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
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

        removeSpeed(damager);
        startStampede(damager);
    }


    @Override
    public void loadSkillConfig() {
        durationPerStack = getConfig("durationPerStack", 4.0, Double.class);
        durationPerStackDecreasePerLevel = getConfig("durationPerStackDecreasePerLevel", 1.0, Double.class);
        damage = getConfig("damage", 0.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        maxSpeedStrength = getConfig("maxSpeedStrength", 3, Integer.class);
        knockbackIncreasePerLevel = getConfig("knockbackIncreasePerLevel", 0.5, Double.class);
        knockback = getConfig("knockback", 0.5, Double.class);
    }
}
