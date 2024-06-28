package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
@BPvPListener
public class HolyLight extends Skill implements PassiveSkill, HealthSkill, TeamSkill, DefensiveSkill, BuffSkill {

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
                getValueString(this::getRadius, level) + " blocks <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength) + "</effect>"
        };
    }

    public double getRadius(int level) {
        return baseRadius + ((level-1) * radiusIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level-1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    private void activate(Player player, int level) {
        championsManager.getEffects().addEffect(player, EffectTypes.REGENERATION, getName(), regenerationStrength, (long) getDuration(level) * 1000, true);
        for (var target : UtilPlayer.getNearbyPlayers(player, player.getLocation(), getRadius(level), EntityProperty.FRIENDLY)) {
            championsManager.getEffects().addEffect(target.getKey(), player, EffectTypes.REGENERATION, getName(), regenerationStrength, (long) getDuration(level) * 1000, true);
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

        regenerationStrength = getConfig("regenerationStrength", 1, Integer.class);
    }
}
