package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class StrengthInNumbers extends Skill implements InteractSkill, CooldownSkill {

    private int radius;
    private double baseDuration;

    private double durationIncreasePerLevel;

    private int strengthLevel;

    @Inject
    public StrengthInNumbers(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Strength in Numbers";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Grant all allies within <stat>" + radius + "</stat> blocks",
                "<effect>Strength " + UtilFormat.getRomanNumeral(strengthLevel) + "</effect> for <val>" + getDuration(level) + "</val> seconds",
                "",
                "This does not give you the buff",
                "",
                "Cooldown: <val>" + getCooldown(level)

        };
    }

    public double getDuration (int level) {
        return baseDuration + (level * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0F, 2.0F);
        championsManager.getEffects().addEffect(player, EffectType.STRENGTH, strengthLevel, (long) (getDuration(level) * 1000L));

        for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), radius)) {
            championsManager.getEffects().addEffect(target, EffectType.STRENGTH, strengthLevel, (long) ((getDuration(level)* 1000L)));
            UtilMessage.message(target, getName(), UtilMessage.deserialize("<yellow>%s</yellow> gave you <white>Strength "+ UtilFormat.getRomanNumeral(strengthLevel) + "</white> for <green>%s</green> seconds.", player.getName(), getDuration(level)));
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 10, Integer.class);
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 2.0, Double.class);
        strengthLevel = getConfig("strengthLevel", 1, Integer.class);
    }
}
