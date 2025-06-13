package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.RayTraceResult;

import java.util.Collection;

@Singleton
@BPvPListener
public class Slash extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill, OffensiveSkill, DamageSkill {

    private final DamageLogManager damageLogManager;

    private double distance;
    private double distanceIncreasePerLevel;
    private double cooldownReduction;
    private double cooldownReductionPerLevel;
    private double damage;
    private double damageIncreasePerLevel;

    @Inject
    public Slash(Champions champions, ChampionsManager championsManager, DamageLogManager damageLogManager) {
        super(champions, championsManager);
        this.damageLogManager = damageLogManager;
    }

    @Override
    public String getName() {
        return "Slash";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Dash forwards " + getValueString(this::getDistance, level) + " blocks, dealing " + getValueString(this::getDamage, level),
                "damage to anything you pass through",
                "",
                "Cooldown resets whenever you kill an enemy",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDistance(int level) {
        return distance + (distanceIncreasePerLevel * (level - 1));
    }

    public double getDamage(int level) {
        return damage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        final Location originalLocation = player.getLocation();
        UtilLocation.teleportForward(player, getDistance(level), false, success -> {
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
                                    .forEach(hit -> hit(player, level, hit)),
                            () -> UtilMessage.message(player, getClassType().getName(), "You missed <alt>%s</alt>.", getName()));
        });
    }

    private void hit(Player caster, int level, LivingEntity hit) {
        CustomDamageEvent cde = new CustomDamageEvent(hit, caster, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Slash");
        cde.setDamageDelay(0);
        UtilDamage.doCustomDamage(cde);

        if (!cde.isCancelled()) {
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT, 0.8f, 2f);
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ITEM_TRIDENT_HIT, 0.8f, 1.5f);

            UtilMessage.message(caster, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", hit.getName(), getName());
            UtilMessage.message(hit, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), getName());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        DamageLog lastDamager = damageLogManager.getLastDamager(event.getEntity());
        if(lastDamager != null && lastDamager.getDamager() instanceof Player player) {
            int level = getLevel(player);
            if(level > 0) {
                championsManager.getCooldowns().removeCooldown(player, getName(), false);
            }
        }

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
    public double getCooldown(int level) {
        return cooldown - (level * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.5, Double.class);
        distance = getConfig("distance", 5.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 0.0, Double.class);
    }
}
