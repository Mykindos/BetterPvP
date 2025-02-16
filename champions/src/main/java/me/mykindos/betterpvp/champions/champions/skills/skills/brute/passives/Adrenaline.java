package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Adrenaline extends Skill implements PassiveSkill, Listener, BuffSkill, OffensiveSkill {

    @Getter
    private double speedOneHealth;
    @Getter
    private double speedTwoHealth;

    private final Set<Player> trackedPlayers = Collections.newSetFromMap(new WeakHashMap<>());

    @Inject
    public Adrenaline(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Adrenaline";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Below <val>" + UtilFormat.formatNumber(getSpeedOneHealth() * 100, 1) + "</val> health you gain <effect>Speed I</effect>,",
                "and below <val>" + UtilFormat.formatNumber(getSpeedTwoHealth() * 100, 1) + "</val> health you gain <effect>Speed II</effect> ",
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @UpdateEvent(delay = 100)
    public void giveSpeed() {
        for (Player player : trackedPlayers) {
            if (player != null && player.isOnline()) {
                if (!hasSkill(player)) {
                    continue;
                }

                double healthThresholdSpeedOne = getSpeedOneHealth() * player.getMaxHealth();
                double healthThresholdSpeedTwo = getSpeedTwoHealth() * player.getMaxHealth();

                if (player.getHealth() <= healthThresholdSpeedTwo) {
                    // Player should have Speed II
                    championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, getName(), 2, 150);
                } else if (player.getHealth() <= healthThresholdSpeedOne) {
                    // Player should have Speed I
                    championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, getName(), 1, 150);
                } else {
                    // Remove Speed effect if present
                    championsManager.getEffects().removeEffect(player, EffectTypes.SPEED, getName());
                }
            }
        }
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        trackedPlayers.remove(player);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        trackedPlayers.add(player);
    }

    @Override
    public void loadSkillConfig() {
        speedOneHealth = getConfig("speedOneHealth", 0.35, Double.class);
        speedTwoHealth = getConfig("speedTwoHealth", 0.15, Double.class);
    }
}
