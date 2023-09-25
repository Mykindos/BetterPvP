package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class RopedArrow extends PrepareArrowSkill {

    @Inject
    public RopedArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Roped Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will pull you",
                "towards the location it hits",
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

    @EventHandler
    public void ArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.contains(arrow)) return;
        if (!hasSkill(player)) return;


        Vector vec = UtilVelocity.getTrajectory(player, arrow);
        double mult = arrow.getVelocity().length() / 3.0D;

        UtilVelocity.velocity(player, vec,
                2.5D + mult, false, 0.4D, 0.3D * mult, 1.5D * mult, true);

        arrow.getWorld().playSound(arrow.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        arrows.remove(arrow);
        championsManager.getEffects().addEffect(player, EffectType.NOFALL, 5000);


    }


    @Override
    public void onHit(Player damager, LivingEntity target, int level) {

    }

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(165, 42, 42).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1));
    }

}
