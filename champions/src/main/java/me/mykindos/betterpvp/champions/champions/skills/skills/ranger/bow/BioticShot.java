package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;


@Singleton
@BPvPListener
public class BioticShot extends PrepareArrowSkill implements HealthSkill, TeamSkill, BuffSkill, DefensiveSkill {

    @Getter
    private double duration;

    @Getter
    private int regenerationStrength;
    @Getter
    private double naturalRegenerationDisabledDuration;
    private final WeakHashMap<Player, Arrow> upwardsArrows = new WeakHashMap<>();
    private final WeakHashMap<Arrow, Vector> initialVelocities = new WeakHashMap<>();

    @Inject
    public BioticShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Biotic Shot";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that gives allies <effect>Regeneration " + UtilFormat.getRomanNumeral(getRegenerationStrength()) + "</effect> for",
                getDuration() + " seconds and cleanses them of all negative effects",
                "",
                "Hitting an enemy with biotic shot will",
                "give them <effect>Anti Heal</effect> for <val>" + getNaturalRegenerationDisabledDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.ANTI_HEAL.getDescription(0)

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
    public void activate(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamageEvent(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!(cde.getProjectile() instanceof Arrow arrow)) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;

        upwardsArrows.remove(damager);

        if (hasSkill(damager)) {
            onHit(damager, cde.getDamagee());
            arrows.remove(arrow);
            arrow.remove();
            cde.addReason(getName());
            if (UtilEntity.isEntityFriendly(damager, cde.getDamagee())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            Vector initialVelocity = arrow.getVelocity();

            double totalMagnitude = initialVelocity.length();

            if (hasSkill(shooter) && initialVelocity.getY() / totalMagnitude >= 0.5) {
                upwardsArrows.put(shooter, arrow);
                initialVelocities.put(arrow, initialVelocity);
            }
        }
    }


    @UpdateEvent
    public void checkPlayerHitboxes() {
        Iterator<Map.Entry<Player, Arrow>> iterator = upwardsArrows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, Arrow> entry = iterator.next();
            Player shooter = entry.getKey();
            Arrow arrow = entry.getValue();
            if (!arrows.contains(arrow)) continue;

            Vector initialVelocity = initialVelocities.get(arrow);

            if (arrow.getVelocity().getY() < 0 && initialVelocity != null && initialVelocity.length() < 0.5) {

                RayTraceResult result = arrow.getWorld().rayTraceEntities(
                        arrow.getLocation(),
                        arrow.getLocation().getDirection(),
                        0.5,
                        0.2,
                        entity -> entity instanceof LivingEntity
                );

                if (result != null && result.getHitEntity() != null && result.getHitEntity().equals(shooter)) {
                    Player target = (Player) result.getHitEntity();
                    onHit(target, target);
                    iterator.remove();
                    initialVelocities.remove(arrow);
                    upwardsArrows.remove(shooter);
                    arrow.remove();
                }
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        Arrow upwardsArrow = upwardsArrows.get(player);
        if (upwardsArrow == null) return;
        if (!upwardsArrow.equals(event.getEntity())) return;

        upwardsArrows.remove(player);
    }


    @Override
    public void onHit(Player damager, LivingEntity target) {
        if (UtilEntity.isEntityFriendly(damager, target)) {
            championsManager.getEffects().addEffect(target, damager, EffectTypes.REGENERATION, getRegenerationStrength(), (long) (getDuration() * 1000));

            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 1.5F);

            championsManager.getEffects().addEffect(target, EffectTypes.IMMUNE, 1);
            UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s</green>", target.getName(), getName()));
            if (!damager.equals(target)) {
                UtilMessage.message(target, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s</green>", damager.getName(), getName()));
            }

        } else {
            championsManager.getEffects().addEffect(target, damager, EffectTypes.ANTI_HEAL, 1, (long) (getNaturalRegenerationDisabledDuration() * 1000));
            UtilMessage.message(target, getClassType().getName(), UtilMessage.deserialize("You hit <alt2>%s</alt2> with <green>%s</green>.", target.getName(), getName()));
            UtilMessage.message(target, getClassType().getName(), UtilMessage.deserialize("<alt2>%s</alt2> hit you with <green>%s</green>.", damager.getName(), getName()));
        }

    }

    @Override
    public void displayTrail(Location location) {
        Particle.GLOW.builder().location(location).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 4.0, Double.class);
        regenerationStrength = getConfig("regenerationStrength", 3, Integer.class);
        naturalRegenerationDisabledDuration = getConfig("naturalRegenerationDisabledDuration", 3.0, Double.class);
    }
}