package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

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
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

@Singleton
@BPvPListener
public class DefensiveAura extends Skill implements InteractSkill, CooldownSkill {

    private double duration;

    @Inject
    public DefensiveAura(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Defensive Aura";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Gives you, and all allies within <val>" + (6 + level) + "</val> blocks",
                "<effect>Health Boost I</effect> for <stat>" + duration + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Set<Role> getClassTypes() {
        return Role.MAGE;
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, (int) duration * 20, 0));
        AttributeInstance playerMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (playerMaxHealth != null) {
            player.setHealth(Math.min(player.getHealth() + 4, playerMaxHealth.getValue()));
            for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), (6 + level))) {

                target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, (int) duration * 20, 0));
                AttributeInstance targetMaxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (targetMaxHealth != null) {
                    target.setHealth(Math.min(target.getHealth() + 4, targetMaxHealth.getValue()));
                }
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 10.0, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
