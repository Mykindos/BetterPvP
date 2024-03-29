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
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
public class ThreateningShout extends Skill implements InteractSkill, CooldownSkill {

    private double radius;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int vulnerabilityStrength;

    @Inject
    public ThreateningShout(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Threatening Shout";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Release a roar, which frightens all",
                "enemies within <val>" + (radius + level) + "</val> blocks and grants",
                "them <effect>Vulnerability " + UtilFormat.getRomanNumeral(vulnerabilityStrength) + "</effect> for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level),
                "",
                EffectTypes.VULNERABILITY.getDescription(vulnerabilityStrength)
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
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
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 2.0F);
        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, player.getLocation(), radius + level)) {
            championsManager.getEffects().addEffect(target, EffectTypes.VULNERABILITY, (long) (getDuration(level) * 1000L));
            UtilMessage.message(target, getName(), "<yellow>%s</yellow> gave you <white>Vulnerability</white> for <green>%s</green> seconds.", player.getName(), getDuration(level));
        }

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 4.0, Double.class);
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 2.0, Double.class);
        vulnerabilityStrength = getConfig("vulnerabilityStrength", 2, Integer.class);
    }
}
