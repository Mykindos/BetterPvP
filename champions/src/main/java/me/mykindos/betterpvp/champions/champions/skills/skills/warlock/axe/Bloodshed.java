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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class Bloodshed extends Skill implements InteractSkill, CooldownSkill, Listener {

   private double baseDuration;

   private double durationIncreasePerLevel;

   private double baseHealthReduction;

   private double healthReductionDecreasePerLevel;

   private int speedStrength;

    @Inject
    public Bloodshed(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bloodshed";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sacrifice <val>" + UtilMath.round(getHealthReduction(level) * 100, 2) + "%" + "</val> of your health to grant",
                "yourself <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength + 1) + "</effect> for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - level * healthReductionDecreasePerLevel;
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
    public void activate(Player player, int level) {
        double healthReduction = 1.0 - getHealthReduction(level);
        double proposedHealth = player.getHealth() - (20 - (20 * healthReduction));

        player.setHealth(Math.max(0.5, proposedHealth));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getDuration(level) * 20), speedStrength));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 2.0f);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level) * cooldownDecreasePerLevel;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseHealthReduction = getConfig("baseHealthReduction", 0.5, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.05, Double.class);

        baseDuration = getConfig("baseDuration", 6.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);

        speedStrength = getConfig("speedStrength", 2, Integer.class);
    }
}
