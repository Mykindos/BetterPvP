package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class PredatorsMark extends Skill implements CooldownToggleSkill, Listener, DamageSkill, OffensiveSkill {

    private final Map<Player, MarkedTarget> marks = new WeakHashMap<>();

    private double baseRange;
    private double rangeIncreasePerLevel;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double baseExtraDamage;
    private double extraDamageIncreasePerLevel;
    private double aimToleranceDegrees;

    @Inject
    public PredatorsMark(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Predators Mark";
    }

    @Override
    public Component[] getDescription(int level) {
        Component range = getValueComponent(this::getRange, level);
        Component duration = getValueComponent(this::getDuration, level);
        Component extraDamage = getValueComponent(this::getExtraDamage, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component glowing = Translations.component("champions.skill.effect.glowing.name").color(NamedTextColor.WHITE);
        return Translations.componentLines(
                "champions.skill.assassin.predators-mark.description",
                range,
                duration,
                extraDamage,
                cooldown,
                glowing
        );
    }

    public double getRange(int level) {
        return baseRange + ((level - 1) * rangeIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getExtraDamage(int level) {
        return baseExtraDamage + ((level - 1) * extraDamageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void toggle(Player player, int level) {
        final LivingEntity target = findTargetedEnemy(player, level);
        if (target == null) {
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.predators-mark.no-target");
            return;
        }

        // Only one mark can be active at a time; replacing it clears the old glow
        clearMark(player);

        marks.put(player, new MarkedTarget(new WeakReference<>(target), System.currentTimeMillis()));
        UtilPlayer.setGlowing(player, target, true);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.5F, 0.6F);
        Particle.SOUL.builder()
                .location(target.getLocation().add(0, 1, 0))
                .count(12)
                .offset(0.4, 0.6, 0.4)
                .extra(0)
                .receivers(60)
                .spawn();

        UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.predators-mark.marked", Component.text(target.getName(), NamedTextColor.YELLOW));
        if (target instanceof Player targetPlayer) {
            UtilMessage.message(targetPlayer, getClassType().getDisplayName(), "champions.skill.assassin.predators-mark.marked-by", Component.text(player.getName(), NamedTextColor.YELLOW));
            showMarkedTitle(targetPlayer, player);
        }
    }

    private void showMarkedTitle(Player targetPlayer, Player marker) {
        final Component titleComponent = Component.text("MARKED", NamedTextColor.RED, TextDecoration.BOLD);
        final Component subtitleComponent = Component.text("by ", NamedTextColor.GRAY)
                .append(Component.text(marker.getName(), NamedTextColor.RED));
        targetPlayer.showTitle(Title.title(titleComponent, subtitleComponent, Title.Times.times(
                Duration.ofMillis(100),
                Duration.ofMillis(1200),
                Duration.ofMillis(400)
        )));
    }

    /**
     * Picks whichever enemy in range is most centered in the player's crosshair (within
     * {@link #aimToleranceDegrees}) and unobstructed, rather than simply the closest one -
     * so in a group fight you can choose to mark the mage in the back instead of whichever
     * enemy happens to be standing nearest to you.
     */
    private @Nullable LivingEntity findTargetedEnemy(Player player, int level) {
        final Location eyeLocation = player.getEyeLocation();
        final Vector direction = eyeLocation.getDirection().normalize();

        LivingEntity best = null;
        double bestAngle = aimToleranceDegrees;

        for (LivingEntity entity : UtilEntity.getNearbyEnemies(player, player.getLocation(), getRange(level))) {
            if (!player.hasLineOfSight(entity)) continue;

            final Vector toEntity = entity.getEyeLocation().toVector().subtract(eyeLocation.toVector());
            final double distance = toEntity.length();
            if (distance < 0.0001) continue;
            toEntity.normalize();

            final double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, direction.dot(toEntity)))));
            if (angle < bestAngle) {
                bestAngle = angle;
                best = entity;
            }
        }
        return best;
    }

    private void clearMark(Player caster) {
        final MarkedTarget removed = marks.remove(caster);
        if (removed == null) return;

        final LivingEntity target = removed.getTarget().get();
        if (target != null) {
            UtilPlayer.setGlowing(caster, target, false);
        }
    }

    private boolean isMarkExpired(Player caster, MarkedTarget marked, int level) {
        return UtilTime.elapsed(marked.getMarkTimestamp(), (long) (getDuration(level) * 1000L));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        final MarkedTarget marked = marks.get(damager);
        if (marked == null) return;

        final LivingEntity target = marked.getTarget().get();
        if (target == null || !target.equals(event.getDamagee())) return;

        final int level = getLevel(damager);
        if (level <= 0 || isMarkExpired(damager, marked, level)) {
            clearMark(damager);
            return;
        }

        event.addModifier(new SkillDamageModifier.Flat(this, getExtraDamage(level)));
        damager.getWorld().playSound(target.getLocation(), Sound.ENTITY_VEX_HURT, 3F, 0.8F);
        damager.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1F, 1.6F);
        Particle.SOUL_FIRE_FLAME.builder()
                .location(target.getLocation().add(0, 1, 0))
                .count(20)
                .offset(0.4, 0.6, 0.4)
                .extra(0.02)
                .receivers(60)
                .spawn();

        UtilMessage.message(damager, getClassType().getDisplayName(), "champions.skill.hit-target", Component.text(target.getName(), NamedTextColor.YELLOW), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        if (event.getDamagee() instanceof Player damagee) {
            UtilMessage.message(damagee, getClassType().getDisplayName(), "champions.skill.hit-by-alt", Component.text(damager.getName(), NamedTextColor.YELLOW), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        }
    }

    @UpdateEvent(delay = 500)
    public void updateMarks() {
        final Iterator<Map.Entry<Player, MarkedTarget>> iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, MarkedTarget> entry = iterator.next();
            final Player caster = entry.getKey();
            final MarkedTarget marked = entry.getValue();
            final LivingEntity target = marked.getTarget().get();
            final int level = getLevel(caster);

            if (!caster.isOnline() || level <= 0 || target == null || !target.isValid() || target.isDead()) {
                if (target != null) {
                    UtilPlayer.setGlowing(caster, target, false);
                }
                iterator.remove();
                continue;
            }

            if (isMarkExpired(caster, marked, level)) {
                UtilPlayer.setGlowing(caster, target, false);
                iterator.remove();
            }
        }
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        clearMark(player);
    }

    @Override
    public void loadSkillConfig() {
        baseRange = getConfig("baseRange", 16.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 2.0, Double.class);
        baseDuration = getConfig("baseDuration", 5.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        baseExtraDamage = getConfig("baseExtraDamage", 2.0, Double.class);
        extraDamageIncreasePerLevel = getConfig("extraDamageIncreasePerLevel", 0.5, Double.class);
        aimToleranceDegrees = getConfig("aimToleranceDegrees", 20.0, Double.class);
    }

    @Value
    private static class MarkedTarget {
        WeakReference<LivingEntity> target;
        long markTimestamp;
    }
}
