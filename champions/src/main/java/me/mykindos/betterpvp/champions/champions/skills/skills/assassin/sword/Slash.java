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
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public Component[] getDescription(int level) {
        Component distance = getValueComponent(this::getDistance, level);
        Component damage = getValueComponent(this::getDamage, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines(
                "champions.skill.assassin.slash.description",
                distance,
                damage,
                cooldown
        );
    }

    public double getDistance(int level) {
        return distance + (distanceIncreasePerLevel * (level - 1));
    }

    public double getDamage(int level) {
        return damage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public boolean activate(Player player, int level) {
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
                            () -> UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.missed", getDisplayName().color(NamedTextColor.GREEN)));
        });
        return true;
    }

    private void hit(Player caster, int level, LivingEntity hit) {
        DamageEvent cde = new DamageEvent(hit, caster, null, new SkillDamageCause(this), getDamage(level), "Slash");
        cde.setDamageDelay(0);
        UtilDamage.doDamage(cde);

        if (!cde.isCancelled()) {
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT, 0.8f, 2f);
            hit.getWorld().playSound(hit.getLocation().add(0, 1, 0), Sound.ITEM_TRIDENT_HIT, 0.8f, 1.5f);

            UtilMessage.message(caster, getClassType().getDisplayName(), "champions.skill.hit-target", this.championsManager.getDisplayNameAsComponent(hit, caster), getDisplayName().color(NamedTextColor.GREEN));
            UtilMessage.message(hit, getClassType().getDisplayName(), "champions.skill.hit-by", this.championsManager.getDisplayNameAsComponent(caster, hit), getDisplayName().color(NamedTextColor.GREEN));
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
