package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;

@Singleton
@BPvPListener
public class IncendiaryShot extends PrepareArrowSkill implements FireSkill, OffensiveSkill {

    private double baseBurnDuration;

    private double burnDurationIncreasePerLevel;

    @Inject
    public IncendiaryShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Incendiary Shot";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an ignited arrow that <effect>Burns</effect>",
                "anyone hit for " + getValueString(this::getBurnDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getBurnDuration(int level) {
        return baseBurnDuration + ((level-1) * burnDurationIncreasePerLevel);
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
        UtilServer.runTaskLater(champions, () -> target.setFireTicks((int) (getBurnDuration(level) * 20)), 2);
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        arrows.add(arrow);
        arrow.setFireTicks(200);
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.FLAME)
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseBurnDuration = getConfig("baseBurnDuration", 1.5, Double.class);
        burnDurationIncreasePerLevel = getConfig("burnDurationIncreasePerLevel", 1.5, Double.class);
    }

}
