package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

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
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class HolyLight extends Skill implements PassiveSkill {

    public double baseRadius;

    public double radiusIncreasePerLevel;

    public int regenerationStrength;

    public double baseDuration;

    public double durationIncreasePerLevel;

    @Inject
    public HolyLight(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Holy Light";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Create an aura that gives",
                "yourself and all allies within",
                "<val>" + getRadius(level) + "</val> blocks <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength + 1) + "</effect>"};
    }

    public double getRadius(int level) {
        return baseRadius + level * radiusIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    private void activate(Player player, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (getDuration(level) * 20), regenerationStrength));
        for (var target : UtilPlayer.getNearbyPlayers(player, player.getLocation(), getRadius(level), EntityProperty.FRIENDLY)) {
            target.getKey().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (getDuration(level) * 20), regenerationStrength));
        }
    }


    @UpdateEvent(delay = 500)
    public void updateHolyLight() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level > 0) {
                activate(player, level);
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 8.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);

        baseDuration = getConfig("baseDuration", 7.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);

        regenerationStrength = getConfig("regenerationStrength", 0, Integer.class);
    }
}
