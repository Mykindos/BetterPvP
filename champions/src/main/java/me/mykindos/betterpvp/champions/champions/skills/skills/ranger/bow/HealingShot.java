package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;

@Singleton
@BPvPListener
public class HealingShot extends PrepareArrowSkill {

    double baseDuration;

    double increaseDurationPerLevel;

    double cooldownDecreasePerLevel;

    @Inject
    public HealingShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Healing Shot";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that gives <effect>Regeneration III</effect>",
                "to allies hit for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        //intentionally left b
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        arrows.add(arrow);
        arrow.setFireTicks(200);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void on

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(255, 0, 0).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level * cooldownDecreasePerLevel));
    }

    public double getDuration(int level) {
        return baseDuration + (level * increaseDurationPerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 6, Double.class);
        increaseDurationPerLevel = getConfig("increasePerLevel", 1, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1, Double.class);
    }
}
