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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
public class ScarletBastion extends Skill implements InteractSkill, CooldownSkill {

    private int radius;
    private double duration;
    private int resistanceStrength;
    private double baseHealthReduction;
    private double healthReductionDecreasePerLevel;

    @Inject
    public ScarletBastion(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Scarlet Bastion";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sacrifice <val>" + UtilMath.round(getHealthReduction(level) * 100, 2) + "%</val> of your health to",
                "grant all allies within <val>" + (radius + (level)) + "</val> blocks",
                "<effect>Resistance " + UtilFormat.getRomanNumeral(resistanceStrength + 1) + "</effect> for <stat>" + duration + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - level * healthReductionDecreasePerLevel;
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
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
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

        player.getWorld().playSound(player.getLocation().add(0.0, -1.0, 0.0), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8F, 2.5F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int) duration * 20, resistanceStrength));
        championsManager.getEffects().addEffect(player, EffectType.RESISTANCE, (long) (duration * 1000));

        for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), (radius + level))) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int) duration * 20, resistanceStrength));
            championsManager.getEffects().addEffect(target, EffectType.RESISTANCE, (long) (duration * 1000));
            UtilMessage.message(target, getClassType().getName(), "You received the spirit of the bear!");
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 5, Integer.class);
        duration = getConfig("duration", 5.0, Double.class);
        resistanceStrength = getConfig("resistanceStrength", 1, Integer.class);

        baseHealthReduction = getConfig("baseHealthReduction", 0.4, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.05, Double.class);
    }

}
