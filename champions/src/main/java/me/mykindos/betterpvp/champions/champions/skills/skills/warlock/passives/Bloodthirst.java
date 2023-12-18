package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class Bloodthirst extends Skill implements PassiveSkill {

    private double baseHealthPercent;

    private double healthPercentIncreasePerLevel;

    private int speedStrength;

    @Inject
    public Bloodthirst(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bloodthirst";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your senses are heightened, allowing you",
                "to detect nearby enemies below <val>" + getHealthPercent(level) * 100 + "%</val> health",
                "",
                "While running towards weak enemies,",
                "you receive <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength + 1) + "</effect>"
        };
    }

    public double getHealthPercent(int level) {
        return baseHealthPercent + level * healthPercentIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @UpdateEvent(delay = 1000)
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            int level = getLevel(player);
            if (level <= 0) continue;

            for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), 50)) {
                if (UtilPlayer.getHealthPercentage(target) < getHealthPercent(level)) {
                    UtilPlayer.setGlowing(player, target, true);

                    // Check if player is running towards target
                    double distanceA = player.getLocation().distance(target.getLocation());
                    double distanceB = player.getLocation().add(player.getLocation().getDirection()).distance(target.getLocation());
                    if (distanceA - distanceB > 0.6) {
                        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                            PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
                            if (speed != null) {
                                if (speed.getAmplifier() < speedStrength) {
                                    player.removePotionEffect(PotionEffectType.SPEED);
                                }
                            }
                        }

                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, speedStrength));
                        UtilSound.playSound(player.getWorld(), player, Sound.ENTITY_WARDEN_HEARTBEAT, 1, 0.2f);
                        break;
                    }
                } else {
                    UtilPlayer.setGlowing(player, target, false);
                }
            }

        }

    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseHealthPercent = getConfig("baseHealthPercent", 0.25, Double.class);
        healthPercentIncreasePerLevel = getConfig("healthPercentIncreasePerLevel", 0.05, Double.class);

        speedStrength = getConfig("speedStrength", 0, Integer.class);
    }
}
