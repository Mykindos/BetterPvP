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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
public class SpiritOfTheBear extends Skill implements InteractSkill, CooldownSkill {

    private int radius;
    private double duration;

    @Inject
    public SpiritOfTheBear(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Spirit of the Bear";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a axe to activate.",
                "",
                "Call upon the spirit of the bear",
                "granting all allies within <val>" + (radius + (level)) + "</val> blocks",
                "Resistance II for 5 seconds.",
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

        return cooldown - ((level - 1) * 2);
    }


    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation().add(0.0, -1.0, 0.0), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8F, 2.5F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 1));
        championsManager.getEffects().addEffect(player, EffectType.RESISTANCE, (long) (duration * 1000));

        for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), (radius + level))) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 1));
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
    }

}
