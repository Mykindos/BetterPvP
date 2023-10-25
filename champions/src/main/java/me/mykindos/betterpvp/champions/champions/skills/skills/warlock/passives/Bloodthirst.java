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
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

@Singleton
@BPvPListener
public class Bloodthirst extends Skill implements PassiveSkill {

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
                "to detect nearby enemies below <val>" + (25 + (5 * level)) + "%</val> health",
                "",
                "While running towards weak enemies,",
                "you receive <effect>Speed I</effect>"
        };
    }

    @Override
    public Set<Role> getClassTypes() {
        return Role.WARLOCK;
    }

    @UpdateEvent(delay = 1000)
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            int level = getLevel(player);
            if (level <= 0) continue;

            for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), 50)) {
                if (UtilPlayer.getHealthPercentage(target) < (25 + (level * 5))) {
                    UtilPlayer.setGlowing(player, target, true);

                    // Check if player is running towards target
                    double distanceA = player.getLocation().distance(target.getLocation());
                    double distanceB = player.getLocation().add(player.getLocation().getDirection()).distance(target.getLocation());
                    if (distanceA - distanceB > 0.6) {
                        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                            PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
                            if (speed != null) {
                                if (speed.getAmplifier() < 1) {
                                    player.removePotionEffect(PotionEffectType.SPEED);
                                }
                            }
                        }

                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 0));
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


}
