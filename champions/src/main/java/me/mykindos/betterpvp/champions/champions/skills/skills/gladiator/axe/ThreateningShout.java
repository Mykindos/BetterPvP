package me.mykindos.betterpvp.champions.champions.skills.skills.gladiator.axe;


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
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
public class ThreateningShout extends Skill implements InteractSkill, CooldownSkill {

    private int radius;
    private double duration;

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
                "Release a roar, which frightens all enemies",
                "within <val>" + (radius + level) + "</val> blocks and grants",
                "them <effect>Vulnerability</effect> for <val>" + (duration + level),
                "seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1));
    }


    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 2.0F);
        for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius + level)) {
            championsManager.getEffects().addEffect(target, EffectType.VULNERABILITY, (long) ((duration + level) * 1000L));
        }

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 4, Integer.class);
        duration = getConfig("duration", 3.0, Double.class);
    }
}
