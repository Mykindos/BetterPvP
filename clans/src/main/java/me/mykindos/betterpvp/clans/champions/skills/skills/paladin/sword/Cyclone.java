package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

@Singleton
public class Cyclone extends Skill implements InteractSkill, CooldownSkill {

    @Inject
    public Cyclone(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Cyclone";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a sword to activate.",
                "",
                "Pulls all enemies within",
                ChatColor.GREEN.toString() + (7 + level) + ChatColor.GRAY + " blocks towards you",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @Override
    public double getCooldown(int level) {

        return 16 - ((level - 1));
    }


    @Override
    public void activate(Player player, int level) {
        Vector vector = player.getLocation().toVector();
        vector.setY(vector.getY() + 2);


        for (LivingEntity target : UtilEntity.getNearbyEntities(player, player.getLocation(), 7 + level, EntityProperty.ENEMY)) {
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
}
