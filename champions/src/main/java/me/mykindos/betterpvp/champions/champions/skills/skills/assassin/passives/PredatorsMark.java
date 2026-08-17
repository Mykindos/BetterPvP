package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.component.PermanentComponent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Predicate;

@Singleton
@BPvPListener
public class PredatorsMark extends Skill implements CooldownToggleSkill, DebuffSkill, MovementSkill, OffensiveSkill {

    private final Map<Player, MarkedTarget> marks = new WeakHashMap<>();

    // Shared crimson identity - every particle this skill spawns pulls from these two colors
    private static final Color MARK_COLOR = Color.fromRGB(200, 0, 30);
    private static final Color MARK_COLOR_FADE = Color.fromRGB(40, 0, 10);

    // Strike-range warning stays silent until the hunter is right on top of the target
    private static final double STRIKE_RANGE = 5.0;

    // Distance from the target the lunge resolves at - short of true melee reach so the caster
    // still has to close the last step themselves instead of landing on/behind the target
    private static final double LUNGE_ARRIVAL_RANGE = 2.0;

    // Players are client-authoritative for movement, so there's lag (roughly a network
    // round-trip) between the server setting velocity and it showing up in getLocation(). Braking
    // hard over this final stretch keeps overshoot to a fraction of a block instead of a full
    // tick's travel, without changing how the rest of the flight (full speed) feels.
    private static final double LUNGE_BRAKING_ZONE = 3.5;
    private static final double LUNGE_BRAKING_SPEED = 0.25;

    private double baseRange;
    private double rangeIncreasePerLevel;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double aimRaySize;

    private double basePursuitBuffer;
    private double pursuitBufferIncreasePerLevel;
    private double lungeSpeedPerTick;
    private int lungeTickSafetyBuffer;

    private double baseLandingDarknessDuration;
    private double landingDarknessDurationIncreasePerLevel;

    // Counts down the window the caster has left to commit before the mark expires
    private final DisplayObject<Component> actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();
        if (player == null || !shouldDisplayActionBar(gamer)) return null;

        final MarkedTarget marked = marks.get(player);
        if (marked == null) return null;

        final LivingEntity target = marked.getTarget().get();
        if (target == null || !target.isValid() || target.isDead()) return null;

        final int level = getLevel(player);
        if (level <= 0) return null;

        final long durationMs = (long) (getDuration(level) * 1000L);
        final long remainingMs = durationMs - (System.currentTimeMillis() - marked.getMarkTimestamp());
        if (remainingMs <= 0) return null; // hide immediately rather than wait on updateMarks()'s sweep
        final float progress = durationMs <= 0 ? 0F : (float) remainingMs / durationMs;

        final ProgressBar bar = ProgressBar.withProgress(progress); // default green/red styling, no icon

        return Component.text(target.getName() + " ", NamedTextColor.YELLOW)
                .append(bar.build())
                .append(Component.text(String.format(" %.1fs", remainingMs / 1000.0), NamedTextColor.GRAY));
    });

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
        Component pursuitBuffer = getValueComponent(this::getPursuitBuffer, level);
        Component landingDarknessDuration = getValueComponent(this::getLandingDarknessDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component glowing = Translations.component("champions.skill.effect.glowing.name").color(NamedTextColor.WHITE);
        Component darkness = Translations.component("champions.skill.effect.darkness.name").color(NamedTextColor.WHITE);
        return Translations.componentLines(
                "champions.skill.assassin.predators-mark.description",
                range,
                duration,
                pursuitBuffer,
                landingDarknessDuration,
                cooldown,
                glowing,
                darkness
        );
    }

    public double getRange(int level) {
        return baseRange + ((level - 1) * rangeIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getPursuitBuffer(int level) {
        return basePursuitBuffer + ((level - 1) * pursuitBufferIncreasePerLevel);
    }

    public double getLandingDarknessDuration(int level) {
        return baseLandingDarknessDuration + ((level - 1) * landingDarknessDurationIncreasePerLevel);
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
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        clearMark(player);
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public void toggle(Player player, int level) {
        final MarkedTarget existingMark = marks.get(player);
        if (existingMark != null) {
            final LivingEntity target = existingMark.getTarget().get();
            if (target != null && target.isValid() && !target.isDead() && !isMarkExpired(player, existingMark, level)) {
                if (existingMark.lunging) return; // already committed, don't stack a second lunge

                // Commit: range capped so the mark's persistence (through walls, for its whole
                // duration) can't be used to sprint off and get flung the whole distance back.
                // Also requires being aimed at the target (same raytrace standard as the initial
                // mark). Either failure burns the mark outright rather than allowing a free retry.
                final double currentGap = player.getLocation().distance(target.getLocation());
                if (currentGap > getRange(level)) {
                    failCommit(player, target, "champions.skill.assassin.predators-mark.too-far");
                    return;
                }
                if (!isAimedAtTarget(player, target, level)) {
                    failCommit(player, target, "champions.skill.assassin.predators-mark.not-facing");
                    return;
                }

                existingMark.lunging = true;
                executeLunge(player, target, level);
                return;
            }

            // The old mark is stale (target gone, or the duration ran out before a commit) -
            // clearMark() puts the ability on cooldown for that unused mark. Stop here rather than
            // falling through to lay a fresh mark in this same press: without the return, a
            // re-press landing in the narrow window right as a mark expires (before updateMarks()'s
            // own sweep catches it) would immediately undo that cooldown via the fresh-mark branch
            // below, which always calls removeCooldown() - letting an expired, never-committed mark
            // be re-marked for free instead of actually costing the cooldown.
            clearMark(player);
            return;
        }

        final LivingEntity target = findTargetedEnemy(player, level);
        if (target == null) {
            playMissEffects(player, level);
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.predators-mark.no-target");
            return;
        }

        final MarkedTarget newMark = new MarkedTarget(new WeakReference<>(target), System.currentTimeMillis());
        marks.put(player, newMark);
        UtilPlayer.setGlowing(player, target, true);

        // Defer the real cooldown - a commit press needs to land within the mark's short duration
        // window; clearMark() reapplies it once the mark's cycle actually concludes
        championsManager.getCooldowns().removeCooldown(player, getName(), true);

        // Precisely time the mark's own expiry instead of leaning on updateMarks()'s 500ms sweep to
        // eventually notice - that sweep is coarse enough that a re-press landing in the gap right
        // as the mark visibly expires still passes the framework's cooldown gate and shows "used"
        // instead of "on cooldown", since nothing has reapplied the cooldown yet at that instant.
        // Scheduling the reapplication for the exact moment the duration ends closes that gap - the
        // identity check guards against clearing a newer mark if this one was already committed (or
        // replaced) before its own timer ran out.
        Bukkit.getScheduler().runTaskLater(champions, () -> {
            if (marks.get(player) == newMark) {
                clearMark(player);
            }
        }, (long) (getDuration(level) * 20));

        playMarkEffects(player, target);
        notifyBoth(player, target, "champions.skill.assassin.predators-mark.marked", "champions.skill.assassin.predators-mark.marked-by");
    }

    // Both failure paths (too far, not aimed) burn the mark outright rather than allowing a retry
    private void failCommit(Player player, LivingEntity target, String messageKey) {
        playMissedCommitEffects(player);
        UtilMessage.message(player, getClassType().getDisplayName(), messageKey, Component.text(target.getName(), NamedTextColor.YELLOW));
        clearMark(player);
    }

    // Tells the caster who they hit/marked, and - if the target is a player - tells them back
    private void notifyBoth(Player caster, LivingEntity target, String casterKey, String targetKey) {
        UtilMessage.message(caster, getClassType().getDisplayName(), casterKey, Component.text(target.getName(), NamedTextColor.YELLOW));
        if (target instanceof Player targetPlayer) {
            UtilMessage.message(targetPlayer, getClassType().getDisplayName(), targetKey, Component.text(caster.getName(), NamedTextColor.YELLOW));
        }
    }

    // Commit: re-activating a live mark launches a homing burst at the target instead of laying a
    // fresh mark. Not an instant teleport (real travel time, re-aims every tick) and not a weak
    // leap (holds a precise, full-speed line). Movement goes through UtilVelocity, the same
    // mechanism every other dash/pull in this game uses (Needlegrasp, Disengage, WolfsPounce).
    private void executeLunge(Player caster, LivingEntity target, int level) {
        // Vanilla PvP collision fights the lunge on approach/landing - disable it for the caster;
        // clearMark() restores it once the mark's cycle concludes
        caster.setCollidable(false);

        notifyBoth(caster, target, "champions.skill.assassin.predators-mark.lunge", "champions.skill.assassin.predators-mark.lunge-target");

        caster.getWorld().playSound(caster.getLocation(), "embandits1:custom.embandit1.macewindup", SoundCategory.AMBIENT, 0.5F, 0.7F);
        caster.playSound(caster.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.7F, 1.6F);
        burstDust(caster.getLocation().add(0, 0.1, 0), 24, 0.3, 0.1, 0.3, 1.3F);

        final double initialGap = caster.getLocation().distance(target.getLocation());

        // Already on top of them - resolve immediately instead of animating a burst that goes nowhere
        if (initialGap <= LUNGE_ARRIVAL_RANGE) {
            landLunge(caster, target, level);
            clearMark(caster);
            return;
        }

        final double distanceBudget = initialGap + getPursuitBuffer(level);

        // Two-phase tick budget - the final LUNGE_BRAKING_ZONE stretch only covers ground at the
        // much slower LUNGE_BRAKING_SPEED, so a flat full-speed estimate underbudgets it enough
        // that the flight could hit maxTicks (and silently fizzle) before actually arriving
        final double cruiseDistance = Math.max(0, distanceBudget - LUNGE_BRAKING_ZONE);
        final int cruiseTicks = (int) Math.ceil(cruiseDistance / lungeSpeedPerTick);
        final int brakingTicks = (int) Math.ceil(LUNGE_BRAKING_ZONE / LUNGE_BRAKING_SPEED);
        final int maxTicks = cruiseTicks + brakingTicks + lungeTickSafetyBuffer;

        new BukkitRunnable() {
            int ticks = 0;
            double traveled = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || !caster.isValid() || !target.isValid() || target.isDead()
                        || !caster.getWorld().equals(target.getWorld()) || getLevel(caster) <= 0
                        || ticks >= maxTicks || traveled >= distanceBudget) {
                    clearMark(caster);
                    cancel();
                    return;
                }

                final Location current = caster.getLocation();
                final Vector toTarget = target.getLocation().toVector().subtract(current.toVector());
                final double distance = toTarget.length();
                if (distance <= LUNGE_ARRIVAL_RANGE) {
                    landLunge(caster, target, level);
                    clearMark(caster);
                    cancel();
                    return;
                }

                // Clamp against the arrival BOUNDARY, not the target itself - lungeSpeedPerTick is
                // almost as large as the standoff distance, so clamping only to the target's exact
                // position would still let a full-speed tick jump clean through the standoff zone.
                // The braking zone caps speed further once close (see field comment for why).
                final double remainingToArrival = distance - LUNGE_ARRIVAL_RANGE;
                final double maxStep = remainingToArrival <= LUNGE_BRAKING_ZONE
                        ? Math.min(lungeSpeedPerTick, LUNGE_BRAKING_SPEED)
                        : lungeSpeedPerTick;
                final double strength = Math.min(maxStep, Math.min(distanceBudget - traveled, remainingToArrival));
                final Vector direction = toTarget.normalize();

                final VelocityData lungeVelocity = new VelocityData(direction, strength, 0.0, 1.5, true);
                UtilVelocity.velocity(caster, null, lungeVelocity);

                traveled += strength;
                ticks++;

                spawnLungeAura(current, ticks);
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    // Orbiting crimson ring + comet-tail behind the caster each tick - pure particles, guaranteed
    // crimson rather than following team-glow rules
    private void spawnLungeAura(Location current, int ticks) {
        final Location center = current.clone().add(0, 1.1, 0);
        final double angle = Math.toRadians((ticks * 34) % 360);
        final double radius = 0.65;
        final double x = Math.cos(angle) * radius;
        final double z = Math.sin(angle) * radius;

        for (Vector offset : new Vector[]{new Vector(x, 0, z), new Vector(-x, 0, -z)}) {
            Particle.DUST.builder()
                    .location(center.clone().add(offset))
                    .count(1)
                    .offset(0, 0, 0)
                    .extra(0)
                    .data(new Particle.DustOptions(MARK_COLOR, 1.0F))
                    .receivers(60)
                    .spawn();
        }

        burstDust(current.clone().add(0, 1, 0), 4, 0.2, 0.3, 0.2, 1.2F);

        Particle.SMOKE.builder()
                .location(current)
                .count(1)
                .offset(0.1, 0.05, 0.1)
                .extra(0.01)
                .receivers(60)
                .spawn();
    }

    // The payoff for closing the gap: a beat of Darkness so the target reads as genuinely
    // disoriented. Deliberately no slow/root on top - that would turn every landed lunge into an
    // inescapable 1v1, which isn't this kit's job. Only fires when the lunge actually lands.
    private void landLunge(Player caster, LivingEntity target, int level) {
        // Hard-stop through UtilVelocity (not a raw setVelocity) so it still participates in the
        // standard velocity pipeline. Direction is irrelevant - zero strength always stops dead -
        // it just has to be non-zero to pass UtilVelocity's own no-op guard.
        UtilVelocity.velocity(caster, null, new VelocityData(caster.getLocation().getDirection(), 0.0, 0.0, 0.0, false));

        championsManager.getEffects().addEffect(target, caster, EffectTypes.DARKNESS, 1, (long) (getLandingDarknessDuration(level) * 1000L));

        caster.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_BITE, 0.5F, 2.0F);
        caster.playSound(caster.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.8F, 1.4F);

        final Location impact = target.getLocation().add(0, 1, 0);
        Particle.SWEEP_ATTACK.builder()
                .location(impact)
                .count(1)
                .extra(0)
                .receivers(60)
                .spawn();

        burstDust(impact, 20, 0.5, 0.4, 0.5, 1.6F);
    }

    // Casting cue: a sparse trail sweeps to the target so onlookers can see who's being marked.
    // Caster gets a personal lock-on sound; target and anyone nearby hear a "you've been found" cue.
    private void playMarkEffects(Player caster, LivingEntity target) {
        spawnTraceEffect(caster.getEyeLocation(), target.getEyeLocation(), MARK_COLOR);

        caster.playSound(caster.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.5F, 0.6F);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.0F, 1.6F);

        burstDust(target.getLocation().add(0, 1, 0), 16, 0.4, 0.6, 0.4, 1.4F);
    }

    // Miss cue: same trace sweep, drawn out to max range along the caster's look direction and
    // capped with a puff. Neutral grey (not the mark's crimson) so a whiff never reads as a hit.
    private void playMissEffects(Player caster, int level) {
        final Location origin = caster.getEyeLocation();
        final Location end = origin.clone().add(origin.getDirection().normalize().multiply(getRange(level)));
        spawnTraceEffect(origin, end, Color.fromRGB(110, 110, 110));

        caster.playSound(caster.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0F, 1.2F);
        caster.getWorld().playSound(end, Sound.BLOCK_FIRE_EXTINGUISH, 0.6F, 1.8F);
        poof(end, 0.15);
    }

    // Failed-commit cue (too far or not aimed) - a fizzle at the caster's own position, since
    // there's no meaningful aim direction to trace here. Both failure paths burn the mark, so
    // this uses the codebase's "scrapped action" sound instead of a plain fail clunk.
    private void playMissedCommitEffects(Player caster) {
        caster.playSound(caster.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
        poof(caster.getLocation().add(0, 1, 0), 0.2);
    }

    // Shared builders behind every crimson dust burst / fizzle-puff this skill spawns
    private void burstDust(Location location, int count, double offsetX, double offsetY, double offsetZ, float size) {
        Particle.DUST_COLOR_TRANSITION.builder()
                .location(location)
                .count(count)
                .offset(offsetX, offsetY, offsetZ)
                .extra(0)
                .data(new Particle.DustTransition(MARK_COLOR, MARK_COLOR_FADE, size))
                .receivers(60)
                .spawn();
    }

    private void poof(Location location, double offset) {
        Particle.POOF.builder()
                .location(location)
                .count(6)
                .offset(offset, offset, offset)
                .extra(0)
                .receivers(40)
                .spawn();
    }

    // Short burst of homing Particle.TRAIL sparks (Mojang's vault/trial-chamber reward particle)
    // from just ahead of the origin toward the destination
    private void spawnTraceEffect(Location origin, Location destination, Color color) {
        final Vector direction = destination.toVector().subtract(origin.toVector());
        final double distance = direction.length();
        if (distance < 0.0001) return;
        direction.normalize();

        final Location start = origin.clone().add(direction.multiply(Math.min(1.75, distance))); // off the caster's own camera
        final Collection<Player> receivers = origin.getNearbyPlayers(50);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 5) {
                    cancel();
                    return;
                }

                Particle.TRAIL.builder()
                        .location(start)
                        .data(new Particle.Trail(destination, color, 10))
                        .offset(0.15, 0.15, 0.15)
                        .count(4)
                        .extra(0)
                        .receivers(receivers)
                        .spawn();

                ticks++;
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    // Real raytrace from the eye along the exact look direction out to getRange() - hasLineOfSight
    // adds the block-aware check interpolateCollision doesn't do on its own, so both marking and
    // committing require actually seeing the target, not just an unobstructed entity raycast
    private Optional<Entity> raytraceHit(Player player, int level, Predicate<Entity> filter) {
        final Location eyeLocation = player.getEyeLocation();
        final Location destination = eyeLocation.clone().add(eyeLocation.getDirection().normalize().multiply(getRange(level)));

        return UtilEntity.interpolateCollision(eyeLocation, destination, (float) aimRaySize, filter)
                .map(RayTraceResult::getHitEntity)
                .filter(player::hasLineOfSight);
    }

    private @Nullable LivingEntity findTargetedEnemy(Player player, int level) {
        return raytraceHit(player, level, entity -> UtilEntity.IS_ENEMY.test(player, entity))
                .map(LivingEntity.class::cast)
                .orElse(null);
    }

    // Same raytrace standard as the initial mark, filtered to this one specific target instead of
    // "any enemy" - committing the lunge is held to the same aim standard as landing the mark
    private boolean isAimedAtTarget(Player caster, LivingEntity target, int level) {
        return raytraceHit(caster, level, target::equals).isPresent();
    }

    // Used by every path that ends a mark while it's still the map's current entry for this
    // caster (commit success/fail, natural expiry via toggle(), invalidatePlayer). updateMarks()'s
    // own expiry sweep removes the map entry itself (via its Iterator, to avoid a concurrent
    // modification) and calls finishMark() directly instead - same cleanup, without a second
    // marks.remove() fighting the iterator.
    private void clearMark(Player caster) {
        final MarkedTarget removed = marks.remove(caster);
        if (removed == null) return;
        finishMark(caster, removed.getTarget().get());
    }

    // The mark's cycle is over (landed, failed, or expired) either way - un-glow, restore
    // collision a short beat later (not instantly, so a just-landed caster/target don't get
    // shoved apart by vanilla collision the instant it's re-enabled), and start the real cooldown
    // that toggle() deferred when this mark was first created. This must fire on every ending,
    // including a mark that simply timed out unused - otherwise the deferred cooldown never comes
    // back and the ability stays permanently off cooldown.
    private void finishMark(Player caster, @Nullable LivingEntity target) {
        if (target != null) {
            UtilPlayer.setGlowing(caster, target, false);
        }

        Bukkit.getScheduler().runTaskLater(champions, () -> {
            if (caster.isOnline()) {
                caster.setCollidable(true);
            }
        }, 10L);

        final int level = getLevel(caster);
        if (level > 0) {
            championsManager.getCooldowns().use(caster, getName(), getCooldown(level), true, true, isCancellable(), this::shouldDisplayActionBar);
        }
    }

    private boolean isMarkExpired(Player caster, MarkedTarget marked, int level) {
        return UtilTime.elapsed(marked.getMarkTimestamp(), (long) (getDuration(level) * 1000L));
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
                iterator.remove();
                finishMark(caster, target);
                continue;
            }

            if (isMarkExpired(caster, marked, level)) {
                iterator.remove();
                finishMark(caster, target);
                continue;
            }

            if (target instanceof Player targetPlayer) {
                playStrikeWarning(caster, targetPlayer, marked);
            }
        }
    }

    // Silence while closing the distance - only within STRIKE_RANGE does the target get any
    // warning at all, one Warden jolt as their one chance to react. Re-arms if the caster backs off.
    private void playStrikeWarning(Player caster, Player target, MarkedTarget marked) {
        if (!target.isOnline() || !caster.getWorld().equals(target.getWorld())) return;

        final double distance = caster.getLocation().distance(target.getLocation());
        if (distance > STRIKE_RANGE) {
            marked.warned = false;
            return;
        }

        if (!marked.warned) {
            target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSEST, 1.0F, 1.0F);
            marked.warned = true;
        }
    }

    @Override
    public void loadSkillConfig() {
        baseRange = getConfig("baseRange", 16.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 2.0, Double.class);
        baseDuration = getConfig("baseDuration", 5.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        aimRaySize = getConfig("aimRaySize", 0.5, Double.class);

        basePursuitBuffer = getConfig("basePursuitBuffer", 4.0, Double.class);
        pursuitBufferIncreasePerLevel = getConfig("pursuitBufferIncreasePerLevel", 1.0, Double.class);
        lungeSpeedPerTick = getConfig("lungeSpeedPerTick", 1.9, Double.class); // above Needlegrasp's 1.5/tick pull
        lungeTickSafetyBuffer = getConfig("lungeTickSafetyBuffer", 10, Integer.class);

        baseLandingDarknessDuration = getConfig("baseLandingDarknessDuration", 1.5, Double.class);
        landingDarknessDurationIncreasePerLevel = getConfig("landingDarknessDurationIncreasePerLevel", 0.25, Double.class);
    }

    // warned: whether the strike-range stinger already fired for this approach (re-arms on backoff)
    // lunging: whether this mark has already been committed, so a duplicate activation can't stack
    // a second lunge on top of one already in flight
    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MarkedTarget {
        private final WeakReference<LivingEntity> target;
        private final long markTimestamp;
        private boolean warned;
        private boolean lunging;
    }
}
