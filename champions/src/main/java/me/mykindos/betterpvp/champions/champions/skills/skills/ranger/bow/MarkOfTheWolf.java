package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class MarkOfTheWolf extends PrepareArrowSkill implements TeamSkill, BuffSkill, DebuffSkill, OffensiveSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double bleedDuration;
    private double bleedDurationIncreasePerLevel;
    private final WeakHashMap<Player, Arrow> upwardsArrows = new WeakHashMap<>();
    private final WeakHashMap<Arrow, Vector> initialVelocities = new WeakHashMap<>();
    private final Map<UUID, MarkedPlayer> markedPlayers = new HashMap<>();

    private static class MarkedPlayer {
        public UUID casterUUID;
        public long markTimestamp;
        public LivingEntity target;
        public MarkedPlayer(UUID casterUUID, long markTimestamp, LivingEntity target) {
            this.casterUUID = casterUUID;
            this.markTimestamp = markTimestamp;
            this.target = target;
        }
    }

    @Inject
    public MarkOfTheWolf(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Mark Of The Wolf";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that gives allies <effect>Mark Of The Wolf",
                "for " + getValueString(this::getDuration, level) + " seconds, causing their next",
                "melee hit to inflict <effect>Bleed</effect> on their target for " + getValueString(this::getBleedDuration, level) + " seconds",
                "",
                "Hitting an enemy with mark of the wolf will give them",
                "<effect>Glowing</effect> and <effect>Darkness</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.BLEED.getDescription(0)
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamageEvent(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!(cde.getProjectile() instanceof Arrow arrow)) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;

        upwardsArrows.remove(damager);

        int level = getLevel(damager);
        if (level > 0) {
            onHit(damager, cde.getDamagee(), level);
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
            int level = getLevel(shooter);

            double totalMagnitude = initialVelocity.length();

            if (level > 0 && initialVelocity.getY() / totalMagnitude >= 0.5) {
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
            if (!arrows.contains(arrow)) return;

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
                    int level = getLevel(shooter);
                    onHit(target, target, level);
                    iterator.remove();
                    initialVelocities.remove(arrow);
                    upwardsArrows.remove(shooter);
                    arrow.remove();
                }
            }
        }
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        markedPlayers.put(target.getUniqueId(), new MarkedPlayer(damager.getUniqueId(), System.currentTimeMillis(), target));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.5f, 2.0f);

        if (!UtilEntity.isEntityFriendly(damager, target)) {
            championsManager.getEffects().addEffect(target, damager, EffectTypes.DARKNESS, 1, (long) (getDuration(level) * 1000L));
            final List<Player> nearbyAllies = UtilPlayer.getNearbyAllies(damager, damager.getLocation(), 45.0);
            show(damager, nearbyAllies, target);
        }

        UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>", target.getName(), getName(), level));
        if (!damager.equals(target) && target instanceof Player targetPlayer) {
            UtilMessage.message(targetPlayer, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));
        }

    }

    @EventHandler
    public void onMarkedHit(CustomDamageEvent event){
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.isCancelled()) return;

        for (Map.Entry<UUID, MarkedPlayer> entry : markedPlayers.entrySet()) {
            if (entry.getValue().target.equals(event.getDamager())) {
                Player casterPlayer = Bukkit.getPlayer((entry.getValue().casterUUID));
                if (casterPlayer == null) return;
                int level = getLevel(casterPlayer);

                if (UtilEntity.isEntityFriendly(casterPlayer, event.getDamager())) {
                    championsManager.getEffects().addEffect(event.getDamagee(), casterPlayer, EffectTypes.BLEED, 1, (long) ((getBleedDuration(level) -  1) * 1000L));
                    markedPlayers.remove(entry.getKey());
                    event.getDamagee().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.5f, 1.0f);
                    UtilMessage.message(event.getDamager(), getClassType().getName(), UtilMessage.deserialize("You bit <yellow>%s</yellow> with <green>%s %s</green>", event.getDamagee().getName(), getName(), level));
                    UtilMessage.message(event.getDamagee(), getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", event.getDamager().getName(), getName(), level));
                }
            }
        }
    }

    @UpdateEvent
    public void onMarkUpdate() {
        long currentTime = System.currentTimeMillis();

        for (Iterator<Map.Entry<UUID, MarkedPlayer>> it = markedPlayers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, MarkedPlayer> entry = it.next();
            long elapsed = currentTime - entry.getValue().markTimestamp;
            Player casterPlayer = Bukkit.getPlayer(entry.getValue().casterUUID);
            LivingEntity target = entry.getValue().target;

            if (casterPlayer == null) {
                it.remove();
                continue;
            }

            if (target == null || target.isDead() || !target.isValid()) {
                it.remove();
                continue;
            }

            int level = getLevel(casterPlayer);

            if (elapsed > getDuration(level) * 1000) {
                if (!UtilEntity.isEntityFriendly(casterPlayer, target)) {
                    hide(casterPlayer, UtilPlayer.getNearbyAllies(casterPlayer, target.getLocation(), 45.0), target);
                }
                it.remove();
            } else if (UtilEntity.isEntityFriendly(casterPlayer, target)) {
                Location loc = target.getLocation();
                new ParticleBuilder(Particle.RAID_OMEN)
                        .location(loc)
                        .count(3)
                        .offset(0.3, 0.6, 0.3)
                        .extra(0)
                        .receivers(30)
                        .spawn();
            }
        }
    }



    private void show(Player player, List<Player> allies, LivingEntity target) {
        UtilPlayer.setGlowing(player, target, true);
        for (Player ally : allies) {
            UtilPlayer.setGlowing(ally, target, true);
        }
    }

    private void hide(Player player, List<Player> allies, LivingEntity target) {
        UtilPlayer.setGlowing(player, target, false);
        for (Player ally : allies) {
            UtilPlayer.setGlowing(ally, target, false);
        }
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.RAID_OMEN)
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

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getBleedDuration(int level) {
        return bleedDuration + ((level - 1) * bleedDurationIncreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        bleedDuration = getConfig("bleedDuration", 3.0, Double.class);
        bleedDurationIncreasePerLevel = getConfig("bleedDurationIncreasePerLevel", 0.0, Double.class);
    }
}