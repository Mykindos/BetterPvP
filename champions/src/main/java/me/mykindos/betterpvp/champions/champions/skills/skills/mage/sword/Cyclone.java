package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

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
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

@Singleton
public class Cyclone extends Skill implements InteractSkill, CooldownSkill {

    private int minimumDistance;

    @Inject
    public Cyclone(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Cyclone";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Pulls all enemies within",
                "<val>" + (minimumDistance + level) + "</val> blocks towards you",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1));
    }


    @Override
    public void activate(Player player, int level) {
        Vector vector = player.getLocation().toVector();
        vector.setY(vector.getY() + 2);


        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, player.getLocation(), minimumDistance + level)) {
            if (!target.getName().equalsIgnoreCase(player.getName())) {
                if (player.hasLineOfSight(target)) {

                    Vector velocity = UtilVelocity.getTrajectory(target, player);
                    // LogManager.addLog(target, player, "Cyclone", 0);
                    UtilVelocity.velocity(target, velocity, 1.2D, false, 0.0D, 0.5D, 4.0D, true);
                }

            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.6F);

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        minimumDistance = getConfig("minimumDistance", 7, Integer.class);
    }
}
