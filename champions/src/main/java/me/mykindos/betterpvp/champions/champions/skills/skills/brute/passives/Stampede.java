package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Stampede extends Skill implements PassiveSkill {

    private final WeakHashMap<Player, Long> sprintTime = new WeakHashMap<>();
    private final WeakHashMap<Player, Integer> sprintStr = new WeakHashMap<>();

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
                "of <effect>Speed " + UtilFormat.getRomanNumeral(maxSpeedStrength + 1) + "</effect>",
                "",
                "Attacking during stampede deals <val>" + getDamage(level) + "</val> bonus",
                "bonus damage and <val>"+ getBonusKnockback(level) + "x</val> extra knockback",
                "per speed level"
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    public double getDamage(int level){
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getBonusKnockback(int level){
        return knockback + ((level - 1) * knockbackIncreasePerLevel);
    }

    public double getDurationPerStack(int level){
        return durationPerStack - ((level - 1) * durationPerStackDecreasePerLevel);
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

    @UpdateEvent(delay = 250)
    public void updateSpeed() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level > 0) {
                if (!sprintTime.containsKey(player)) {
                    sprintTime.put(player, System.currentTimeMillis());
                    sprintStr.put(player, -1);
                }

                if (!player.isSprinting() || player.isInWater()) {
                    sprintTime.remove(player);
                    int str = sprintStr.remove(player);

                    if(!UtilPlayer.hasPotionEffect(player, PotionEffectType.SPEED, str + 2)) {
                        player.removePotionEffect(PotionEffectType.SPEED);
                    }
                } else {
                    long time = sprintTime.get(player);
                    int str = sprintStr.get(player);
                    if (str >= 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, str));
                    }
                    if (UtilTime.elapsed(time, (long) ((durationPerStack - level) * 1000L))) {
                        sprintTime.put(player, System.currentTimeMillis());
                        if (str < maxSpeedStrength) {
                            sprintStr.put(player, str + 1);

                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2.0F, 0.2F * str + 1.2F);
                            UtilMessage.simpleMessage(player, getClassType().getName(), "Stampede Level: <yellow>%d", (str + 2));
                        }
                    }
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {

        if (event.getDamager() instanceof Player damager) {

            if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
            int str = sprintStr.getOrDefault(damager, 0);
            if (str < 1) return;

            int level = getLevel(damager);

            sprintTime.remove(damager);
            sprintStr.remove(damager);
            damager.removePotionEffect(PotionEffectType.SPEED);

            double bonusKnockback = getBonusKnockback(level);

            event.setKnockback(false);
            VelocityData velocityData = new VelocityData(UtilVelocity.getTrajectory2d(damager, event.getDamagee()), bonusKnockback, true, 0.0D, 0.4D, 1.0D, true);
            UtilVelocity.velocity(event.getDamagee(), damager, velocityData, VelocityType.KNOCKBACK);

            double additionalDamage = (str + 1) * getDamage(level);
            event.setDamage(event.getDamage() + additionalDamage);
        } else if (event.getDamagee() instanceof Player player) {
            int str = sprintStr.getOrDefault(player, 0);
            if (str < 1) return;

            if(player.hasPotionEffect(PotionEffectType.SPEED)) {
                player.removePotionEffect(PotionEffectType.SPEED);
                sprintTime.remove(player);
                sprintStr.remove(player);
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        durationPerStack = getConfig("durationPerStack", 4.0, Double.class);
        durationPerStackDecreasePerLevel = getConfig("durationPerStackDecreasePerLevel", 1.0, Double.class);
        damage = getConfig("damage", 0.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        maxSpeedStrength = getConfig("maxSpeedStrength", 2, Integer.class);
        knockbackIncreasePerLevel = getConfig("knockbackIncreasePerLevel", 0.5, Double.class);
        knockback = getConfig("knockback", 0.5, Double.class);
    }
}
