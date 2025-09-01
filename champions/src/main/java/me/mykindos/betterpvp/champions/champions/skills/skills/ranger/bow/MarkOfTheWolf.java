package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class MarkOfTheWolf extends PrepareArrowSkill implements TeamSkill, BuffSkill, DebuffSkill, OffensiveSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double baseExtraDamage;
    private double extraDamageIncreasePerLevel;
    private int speedStrength;
    private final WeakHashMap<Player, Arrow> upwardsArrows = new WeakHashMap<>();
    private final WeakHashMap<Arrow, Vector> initialVelocities = new WeakHashMap<>();
    private final Map<LivingEntity, MarkedPlayer> markedPlayers = new WeakHashMap<>();

    private static class MarkedPlayer {
        private final UUID casterUUID;
        private final long markTimestamp;
        private final WeakReference<LivingEntity> target;

        public MarkedPlayer(UUID casterUUID, long markTimestamp, LivingEntity target) {
            this.casterUUID = casterUUID;
            this.markTimestamp = markTimestamp;
            this.target = new WeakReference<>(target);
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
                "Shoot an arrow that gives hit players",
                "<effect>Mark Of The Wolf</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Allies will gain <effect>Speed " + UtilFormat.getRomanNumeral(getSpeedStrength(level)) + " </effect> and their next melee",
                "hit will deal <effect>" + getValueString(this::getExtraDamage, level) + "</effect> extra damage",
                "",
                "Enemies will be given <effect>Glowing",
                "and <effect>Darkness</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
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
    public void onPreDamageEvent(DamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!event.isDamageeLiving()) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;

        upwardsArrows.remove(damager);

        int level = getLevel(damager);
        if (level > 0) {
            final LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
            onHit(damager, damagee, level);
            arrows.remove(arrow);
            arrow.remove();
            event.addReason(getName());
            if (UtilEntity.isEntityFriendly(damager, damagee)) {
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
        markedPlayers.put(target, new MarkedPlayer(damager.getUniqueId(), System.currentTimeMillis(), target));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.5f, 2.0f);

        if (!UtilEntity.isEntityFriendly(damager, target)) {
            championsManager.getEffects().addEffect(target, damager, EffectTypes.DARKNESS, 1, (long) (getDuration(level) * 1000L));
            final List<Player> nearbyAllies = UtilPlayer.getNearbyAllies(damager, damager.getLocation(), 45.0);
            show(damager, nearbyAllies, target);
        } else if (UtilEntity.isEntityFriendly(damager, target)) {
            championsManager.getEffects().addEffect(target, damager, EffectTypes.SPEED, getSpeedStrength(level), (long) (getDuration(level) * 1000L));
        }

        UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>", target.getName(), getName(), level));
        if (!damager.equals(target) && target instanceof Player targetPlayer) {
            UtilMessage.message(targetPlayer, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));
        }

    }

    @EventHandler
    public void onMarkedHit(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (event.isCancelled()) return;

        Iterator<Map.Entry<LivingEntity, MarkedPlayer>> iterator = markedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, MarkedPlayer> entry = iterator.next();
            if (entry.getValue().target.equals(event.getDamager())) {
                Player casterPlayer = Bukkit.getPlayer((entry.getValue().casterUUID));
                if (casterPlayer == null) return;
                int level = getLevel(casterPlayer);

                if (UtilEntity.isEntityFriendly(casterPlayer, event.getDamager())) {
                    event.addModifier(new SkillDamageModifier.Flat(this, getExtraDamage(level)));
                    iterator.remove();
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

        for (Iterator<Map.Entry<LivingEntity, MarkedPlayer>> it = markedPlayers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<LivingEntity, MarkedPlayer> entry = it.next();
            long elapsed = currentTime - entry.getValue().markTimestamp;
            Player casterPlayer = Bukkit.getPlayer(entry.getValue().casterUUID);
            LivingEntity target = entry.getValue().target.get();

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

    @UpdateEvent(delay = 5000)
    public void cleanupCollections() {
        markedPlayers.entrySet().removeIf(entry -> {
            LivingEntity livingEntity = entry.getValue().target.get();
            return livingEntity == null || !livingEntity.isValid();
        });
        upwardsArrows.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null || !entry.getValue().isValid());
        initialVelocities.entrySet().removeIf(entry -> entry.getKey() == null || !entry.getKey().isValid() || entry.getValue() == null);
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

    public double getExtraDamage(int level) {
        return baseExtraDamage + ((level - 1) * extraDamageIncreasePerLevel);
    }

    public int getSpeedStrength(int level) {
        return speedStrength;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        baseExtraDamage = getConfig("baseExtraDamage", 2.0, Double.class);
        extraDamageIncreasePerLevel = getConfig("extraDamageIncreasePerLevel", 0.5, Double.class);
        speedStrength = getConfig("speedStrength", 2, Integer.class);
    }
}