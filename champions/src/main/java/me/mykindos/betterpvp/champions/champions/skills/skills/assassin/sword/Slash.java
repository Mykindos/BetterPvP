package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;

import java.util.Collection;

@Singleton
@BPvPListener
public class Slash extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill, OffensiveSkill, DamageSkill {

    @Getter
    private double distance;
    private double cooldownReduction;
    @Getter
    private double damage;

    @Inject
    public Slash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Slash";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Dash forwards <val>" + getDistance() + "</val> blocks, dealing <val>" + getDamage(),
                "damage to anything you pass through",
                "",
                "Every hit will reduce the cooldown by <val>" + getCooldownDecrease() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown()
        };
    }

    public double getCooldownDecrease() {
        return cooldownReduction;
    }

    @Override
    public void activate(Player player) {
        final Location originalLocation = player.getLocation();
        UtilLocation.teleportForward(player, getDistance(), false, success -> {
            final Location lineStart = originalLocation.add(0.0, player.getHeight() / 2, 0.0);
            Particle.SWEEP_ATTACK.builder()
                    .location(lineStart.clone().add(player.getLocation().getDirection()))
                    .count(1)
                    .receivers(30)
                    .extra(0)
                    .spawn();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);

            if (Boolean.FALSE.equals(success)) {
                return;
            }

            final Location teleportLocation = player.getLocation();
            final Location lineEnd = teleportLocation.clone().add(0.0, player.getHeight() / 2, 0.0);
            final VectorLine line = VectorLine.withStepSize(lineStart, lineEnd, 0.25f);
            final Collection<Player> receivers = teleportLocation.getNearbyPlayers(30);
            for (Location point : line.toLocations()) {
                Particle.CRIT.builder()
                        .location(point)
                        .count(2)
                        .receivers(receivers)
                        .extra(0)
                        .spawn();
            }

            // Collision
            UtilEntity.interpolateMultiCollision(originalLocation,
                            teleportLocation,
                            0.5f,
                            ent -> UtilEntity.IS_ENEMY.test(player, ent))
                    .map(MultiRayTraceResult::stream)
                    .ifPresentOrElse(stream -> stream.map(RayTraceResult::getHitEntity)
                                    .map(LivingEntity.class::cast)
                                    .forEach(hit -> hit(player, hit)),
                            () -> UtilMessage.message(player, getClassType().getName(), "You missed <alt>%s</alt>.", getName()));
        });
    }

    private void hit(Player caster, LivingEntity hit) {
        CustomDamageEvent cde = new CustomDamageEvent(hit, caster, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(), false, "Slash");
        cde.setDamageDelay(0);
        UtilDamage.doCustomDamage(cde);

        if (!cde.isCancelled()) {
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT, 0.8f, 2f);
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ITEM_TRIDENT_HIT, 0.8f, 1.5f);

            UtilMessage.message(caster, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", hit.getName(), getName());
            UtilMessage.message(hit, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && !event.hasReason("Slash")) {
            return;
        }

        this.championsManager.getCooldowns().reduceCooldown(player, getName(), getCooldownDecrease());
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
        distance = getConfig("distance", 5.0, Double.class);
        cooldownReduction = getConfig("cooldownReduction", 3.0, Double.class);
    }
}
