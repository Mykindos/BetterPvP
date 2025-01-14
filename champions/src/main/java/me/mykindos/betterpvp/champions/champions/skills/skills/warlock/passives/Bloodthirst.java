package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@Singleton
@BPvPListener
public class Bloodthirst extends Skill implements PassiveSkill, MovementSkill, BuffSkill {

    @Getter
    private double healthPercent;
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
    public String[] getDescription() {
        return new String[]{
                "Your senses are heightened, allowing you",
                "to detect nearby enemies below <val>" + UtilFormat.formatNumber(getHealthPercent() * 100, 0) + "%</val> health",
                "",
                "While running towards weak enemies,",
                "you receive <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect>"
        };
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @UpdateEvent(delay = 1000)
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!hasSkill(player)) continue;

            for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), 50)) {
                if (UtilPlayer.getHealthPercentage(target) < getHealthPercent()) {
                    UtilPlayer.setGlowing(player, target, true);

                    // Check if player is running towards target
                    double distanceA = player.getLocation().distance(target.getLocation());
                    double distanceB = player.getLocation().add(player.getLocation().getDirection()).distance(target.getLocation());
                    if (distanceA - distanceB > 0.6) {
                        championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, speedStrength, 1500);
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
        healthPercent = getConfig("healthPercent", 0.30, Double.class);

        speedStrength = getConfig("speedStrength", 2, Integer.class);
    }
}
