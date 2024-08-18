package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe.Clone;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class StormSphere extends PrepareArrowSkill implements FireSkill, OffensiveSkill {

    private final HashMap<Player, StormData> activeSpheres = new HashMap<>();

    @Inject
    public StormSphere(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Storm Sphere";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an ignited arrow that <effect>Burns</effect>",
                "anyone hit for  +  seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
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
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND, 0.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<Player, StormData>> it = activeSpheres.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, StormData> entry = it.next();
            Player player = entry.getKey();

            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            int level = getLevel(player);

            if (level <= 0) {
                it.remove();
                continue;
            }

            long duration = entry.getValue().getDuration();

            if (UtilTime.elapsed(duration, (long) 3 * 1000)) {
                it.remove();
            }

            Location location = entry.getValue().getLocation();

            for (LivingEntity target : UtilEntity.getNearbyEnemies(player, location, 6.0)) {
                championsManager.getEffects().addEffect(target, player, EffectTypes.SHOCK, 50L);
                championsManager.getEffects().addEffect(target, EffectTypes.SILENCE, 50L);
            }

        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.contains(arrow)) return;
        if (!hasSkill(player)) return;

        final Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 60);

        for (Location point : UtilLocation.getSphere(arrow.getLocation(), 6.0, 25)) {
            new ParticleBuilder(Particle.DRIPPING_WATER)
                    .location(point)
                    .count(1)
                    .extra(1)
                    .source(player)
                    .receivers(receivers)
                    .spawn();
        }

        activeSpheres.put(player, new StormData(System.currentTimeMillis(), arrow.getLocation()));
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        target.getWorld().strikeLightning(target.getLocation());
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.DRIPPING_WATER)
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

    @AllArgsConstructor
    @Getter
    private class StormData {
        private final long duration;
        private final Location location;
    }
}
