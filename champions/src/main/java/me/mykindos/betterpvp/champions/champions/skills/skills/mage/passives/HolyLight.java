package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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

    @Getter
    public double radius;
    public int regenerationStrength;
    @Getter
    public double duration;

    @Inject
    public HolyLight(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Holy Light";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Create an aura that gives",
                "yourself and all allies within",
                getRadius() + " blocks <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength) + "</effect>"
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    private void activate(Player player) {
        championsManager.getEffects().addEffect(player, EffectTypes.REGENERATION, getName(), regenerationStrength, (long) getDuration() * 1000, true);
        for (var target : UtilPlayer.getNearbyPlayers(player, player.getLocation(), getRadius(), EntityProperty.FRIENDLY)) {
            championsManager.getEffects().addEffect(target.getKey(), player, EffectTypes.REGENERATION, getName(), regenerationStrength, (long) getDuration() * 1000, true);
        }
    }


    @UpdateEvent(delay = 500)
    public void updateHolyLight() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasSkill(player)) {
                activate(player);
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 8.0, Double.class);

        duration = getConfig("duration", 7.0, Double.class);

        regenerationStrength = getConfig("regenerationStrength", 1, Integer.class);
    }
}
