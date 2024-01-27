package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class Cleanse extends Skill implements InteractSkill, CooldownSkill, Listener {

    private double baseDuration;

    private double durationIncreasePerLevel;
    private double baseRange;

    private double rangeIncreasePerLevel;

    private double baseHealthReduction;

    private double healthReductionDecreasePerLevel;
    @Inject
    public Cleanse(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Cleanse";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sacrifice <val>" + UtilMath.round(getHealthReduction(level) * 100, 2) + "%" + "</val> of your health to purge all negative",
                "effects from yourself and allies within <val>" + getRange(level) + "</val> blocks",
                "",
                "You and your allies also receive an immunity against negative",
                "effects for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - level * healthReductionDecreasePerLevel;
    }

    public double getRange(int level) {
        return baseRange + level * rangeIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }


    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (20 - (20 * healthReduction));

        if (proposedHealth <= 0.5) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName(), level);
            return false;
        }

        return true;
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - level * cooldownDecreasePerLevel;
    }

    @Override
    public void activate(Player player, int level) {
        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (20 - (20 * healthReduction));

        player.setHealth(Math.max(0.5, proposedHealth));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 1.0f, 0.9f);
        championsManager.getEffects().addEffect(player, EffectType.IMMUNETOEFFECTS, (long) (getDuration(level) * 1000L));

        for (Player ally : UtilPlayer.getNearbyAllies(player, player.getLocation(), getRange(level))) {
            championsManager.getEffects().addEffect(ally, EffectType.IMMUNETOEFFECTS, (long) (getDuration(level) * 1000L));
            UtilMessage.simpleMessage(ally, "Cleanse", "You were cleansed of negative by <alt>" + player.getName());
            UtilServer.callEvent(new EffectClearEvent(ally));
        }


        UtilServer.callEvent(new EffectClearEvent(player));
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean ignoreNegativeEffects() {
        return true;
    }

    @Override
    public void loadSkillConfig() {
        baseHealthReduction = getConfig("baseHealthReduction", 0.4, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.05, Double.class);
        baseRange = getConfig("baseRange", 5.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 1.0, Double.class);

        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
    }
}
