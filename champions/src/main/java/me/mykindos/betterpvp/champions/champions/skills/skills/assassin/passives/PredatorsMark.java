package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Assassin passive. Drop a weapon to mark the enemy in your crosshair: for a few seconds only you see them,
 * glowing through walls. Drop again within that window to blind them and lunge toward them, stopping short. Land the kill before
 * the hunt timer runs out and a short window opens to drop once more - dashing back to the spot you leapt from.
 */
@Singleton
@BPvPListener
@CustomLog
public class PredatorsMark extends Skill implements CooldownToggleSkill, MovementSkill, DebuffSkill, Listener {

    // Active sequence - one phase at a time per hunter.
    private final Map<Player, Mark> marks = new WeakHashMap<>();
    private final Map<Player, Lunge> lunges = new WeakHashMap<>();
    private final Map<Player, Location> lungeArrivals = new WeakHashMap<>(); // internal arrival teleports
    private final Map<Player, Hunt> hunts = new WeakHashMap<>();              // stuck lunge; kill the target before it lapses to earn the shadowstep
    private final Map<Player, Shadowstep> shadowsteps = new WeakHashMap<>();  // open "drop again to escape" window
    private final Map<Player, ReturnDash> returnDashes = new WeakHashMap<>(); // escape dash in flight

    // Passive preview - a screen-edge vignette while an enemy is markable in the crosshair.
    private final Set<Player> equippedPlayers = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<Player, Player> vignetteTargets = new WeakHashMap<>();  // current preview target, so its death/quit clears the vignette at once

    private double range;
    private double markDuration;      // glow lifetime, and the window to re-drop and lunge
    private double blindDuration;     // blindness on the target when the hunter commits the lunge
    private double lungeSpeed;
    private double lungeStopDistance; // land this far short of the target - a dive that ends inside them feels bad
    private double maxLungeDuration;  // hard timeout on an in-flight lunge or return dash
    private double huntDuration;      // time after a stuck lunge to secure the kill and earn the escape
    private double returnWindow;      // time after the kill to drop again and dash home
    private double returnMaxDistance; // give up the escape if the hunter is dragged this far from the anchor

    @Inject
    public PredatorsMark(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Predators Mark";
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
    public Component[] getDescription(int level) {
        Component blindness = Translations.component("champions.skill.effect.blindness.name").color(NamedTextColor.WHITE);
        return Translations.componentLines("champions.skill.assassin.predators-mark.description",
                getValueComponent(ignored -> range, level),           // {0}
                getValueComponent(ignored -> markDuration, level),     // {1}
                getValueComponent(ignored -> blindDuration, level),    // {2}
                getValueComponent(ignored -> huntDuration, level),     // {3}
                getValueComponent(this::getCooldown, level),           // {4}
                blindness);                                            // {5}
    }

    @Override
    public double getCooldown(int level) {
        return cooldown;
    }

    @Override
    public boolean isDelayedSkill() {
        return true;
    }

    @Override
    public boolean isUsingSkill(Player player) {
        // The whole sequence counts as "in use" so the delayed-cooldown flow never starts the cooldown itself -
        // it is committed by hand at each end point (see startCooldown callers), so it does not tick during the
        // hunt or the escape window.
        return marks.containsKey(player) || lunges.containsKey(player) || hunts.containsKey(player)
                || shadowsteps.containsKey(player) || returnDashes.containsKey(player);
    }

    @Override
    public boolean canReactivate(Player player) {
        final long now = System.currentTimeMillis();
        Mark mark = marks.get(player);
        if (mark != null && now < mark.expires) return true;
        Shadowstep step = shadowsteps.get(player);
        return step != null && now < step.expires();
    }

    @Override
    public boolean canUse(Player player) {
        if (lunges.containsKey(player) || hunts.containsKey(player) || returnDashes.containsKey(player)
                || player.getGameMode() == GameMode.SPECTATOR) return false;
        if (shadowsteps.containsKey(player)) return true;
        if (marks.containsKey(player) || championsManager.getCooldowns().hasCooldown(player, getName())) return true;
        if (findTarget(player) != null) return true;
        message(player, "no-target");
        return false;
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    @Override
    public void toggle(Player player, int level) {
        // Third drop: spend the shadowstep window.
        Shadowstep step = shadowsteps.remove(player);
        if (step != null) {
            if (System.currentTimeMillis() < step.expires()) {
                performShadowstep(player, step);
            }
            return;
        }

        // Second drop: spend the mark on a lunge.
        Mark mark = marks.get(player);
        if (mark != null) {
            if (System.currentTimeMillis() >= mark.expires || !validTarget(player, mark.target)) {
                markLostCue(player);
                endMark(player, true);
                return;
            }
            if (player.getLocation().distanceSquared(mark.target.getLocation()) > range * range) {
                message(player, "out-of-range");
                return;
            }
            Player lungeTarget = mark.target;
            Location anchor = player.getLocation().clone(); // leap point, carried to the shadowstep if this dive lands a kill
            endMark(player, false); // the lunge/hunt/escape chain now owns the cooldown
            // Blindness on commit, not on arrival - the lunge itself can still be walled off.
            championsManager.getEffects().addEffect(lungeTarget, player, EffectTypes.BLINDNESS, 1,
                    (long) (blindDuration * 1000));
            Lunge lunge = new Lunge(lungeTarget, player.getWorld(), anchor,
                    System.currentTimeMillis() + (long) (maxLungeDuration * 1000));
            lunges.put(player, lunge);
            playLungeStart(player);
            advanceLunge(player, lunge);
            return;
        }

        // First drop: mark the enemy in the crosshair.
        Player target = findTarget(player);
        if (target == null) {
            message(player, "no-target");
            return;
        }
        long now = System.currentTimeMillis();
        Mark created = new Mark(target, now + (long) (markDuration * 1000));
        marks.put(player, created);
        updateGlow(player, created);
        UtilMessage.message(player, getClassType().getDisplayName(),
                "champions.skill.assassin.predators-mark.marked", Component.text(target.getName(), NamedTextColor.RED));
        Component targetAlert = Translations.component("champions.skill.assassin.predators-mark.marked-by",
                Component.text(player.getName(), NamedTextColor.RED));
        UtilMessage.message(target, getClassType().getDisplayName(), Translations.render(targetAlert, target.locale()));
        showLungeWindowBar(player, created);
        updateVignette(player, target);
        playMarkApplied(player, target);
    }

    private Player findTarget(Player player) {
        List<Player> enemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), range);
        RayTraceResult hit = player.getWorld().rayTrace(player.getEyeLocation(),
                player.getEyeLocation().getDirection(), range, FluidCollisionMode.NEVER, true, 0,
                entity -> entity instanceof Player target && enemies.contains(target) && validTarget(player, target));
        return hit != null && hit.getHitEntity() instanceof Player target ? target : null;
    }

    private boolean validTarget(Player player, Player target) {
        return player.isOnline() && !player.isDead() && target.isOnline() && !target.isDead()
                && player.getGameMode() != GameMode.SPECTATOR
                && player.getWorld().equals(target.getWorld()) && player.canSee(target)
                && target.getGameMode() != GameMode.SPECTATOR
                && UtilPlayer.getNearbyEnemies(player, target.getLocation(), 1).contains(target);
    }

    @UpdateEvent
    public void update() {
        long now = System.currentTimeMillis();

        // Marks: keep the glow and cues alive, or end the mark (and spend the cooldown) if it lapses.
        for (Player player : List.copyOf(marks.keySet())) {
            Mark mark = marks.get(player);
            if (!isEnabled() || getLevel(player) <= 0) {
                endMark(player, false); // skill switched off or unequipped - no cooldown penalty
                continue;
            }
            if (!validTarget(player, mark.target)) {
                markLostCue(player);
                endMark(player, true); // target escaped or died - marking still commits the cooldown
                continue;
            }
            if (now >= mark.expires) {
                markLostCue(player);
                endMark(player, true);
                continue;
            }
            updateGlow(player, mark);
            pulseReticle(player, mark, now);
            updateVignette(player, player.getLocation().distanceSquared(mark.target.getLocation()) <= range * range
                    ? mark.target : null);
        }

        // Drive the in-flight movement and age out the timed states.
        for (Player player : List.copyOf(lunges.keySet())) {
            advanceLunge(player, lunges.get(player));
        }
        for (Player player : List.copyOf(returnDashes.keySet())) {
            advanceReturn(player, returnDashes.get(player));
        }
        for (Player player : List.copyOf(hunts.keySet())) {
            if (now >= hunts.get(player).expires()) {
                hunts.remove(player);
                if (player.isOnline()) {
                    startCooldown(player); // hunt lapsed without a kill - commit the cooldown now
                }
            }
        }
        for (Player player : List.copyOf(shadowsteps.keySet())) {
            if (now >= shadowsteps.get(player).expires()) {
                shadowsteps.remove(player);
                if (player.isOnline()) {
                    startCooldown(player); // escape window missed - commit the cooldown now
                    new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f).play(player);
                }
            }
        }
        // Passive preview: show the vignette to anyone holding the weapon with a markable enemy in the crosshair.
        for (Player player : List.copyOf(equippedPlayers)) {
            if (!player.isOnline() || getLevel(player) <= 0) {
                invalidatePlayer(player, null);
                continue;
            }
            if (marks.containsKey(player)) continue;
            final SkillType held = SkillWeapons.getTypeFrom(player.getInventory().getItemInMainHand());
            boolean ready = isEnabled() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                    && !lunges.containsKey(player)
                    && !championsManager.getCooldowns().hasCooldown(player, getName())
                    && (held == SkillType.SWORD || held == SkillType.AXE);
            updateVignette(player, ready ? findTarget(player) : null);
        }
    }

    private void updateGlow(Player player, Mark mark) {
        if (mark.glowing || mark.glowFailed) return;
        try {
            UtilPlayer.setGlowing(player, mark.target, true);
            mark.glowing = true;
        } catch (Exception ex) {
            mark.glowFailed = true;
            log.warn("Could not apply Predator's Mark glow", ex).submit();
        }
    }

    private void clearGlow(Player player, Mark mark) {
        if (!mark.glowing && !mark.glowFailed) return;
        mark.glowing = false;
        mark.glowFailed = false;
        try {
            UtilPlayer.setGlowing(player, mark.target, false);
        } catch (Exception ex) {
            log.warn("Could not clear Predator's Mark glow", ex).submit();
        }
    }

    private void updateVignette(Player player, Player target) {
        Player previous = target == null ? vignetteTargets.remove(player) : vignetteTargets.put(player, target);
        if ((previous == null) == (target == null)) return;
        if (!player.isOnline()) return;
        try {
            if (target != null) UtilPlayer.setWarningEffect(player, 1);
            else UtilPlayer.clearWarningEffect(player);
        } catch (Exception ex) {
            log.warn("Could not update Predator's Mark vignette", ex).submit();
        }
    }

    private void advanceLunge(Player player, Lunge lunge) {
        if (!isEnabled() || getLevel(player) <= 0 || !validTarget(player, lunge.target())
                || !player.getWorld().equals(lunge.world()) || System.currentTimeMillis() >= lunge.expires()
                || player.getLocation().distanceSquared(lunge.target().getLocation()) > range * range) {
            fizzleLunge(player);
            return;
        }
        final Location start = player.getLocation();
        final Vector toTarget = lunge.target().getLocation().toVector().subtract(start.toVector());
        final double distance = toTarget.length();
        final double targetFeetY = lunge.target().getLocation().getY();
        if (distance <= lungeStopDistance + 0.1) {
            // Include a small tolerance so eased movement cannot stall just outside the stop point.
            if (player.hasLineOfSight(lunge.target())) {
                finishLunge(player, lunge);
            } else {
                fizzleLunge(player); // ducked behind cover at the last moment
            }
            return;
        }
        // Aim for a point lungeStopDistance short of the target so the dive never ends inside them.
        final double remaining = distance - lungeStopDistance;
        final double speed = Math.min(lungeSpeed, remaining <= 1.5 ? Math.min(0.4, remaining) : remaining);
        final Vector velocity = toTarget.multiply(speed / distance);
        final CustomEntityVelocityEvent event = UtilServer.callEvent(
                new CustomEntityVelocityEvent(player, null, VelocityType.CUSTOM, velocity));
        if (event.isCancelled() || !clearPath(player, event.getVector(), targetFeetY)) {
            fizzleLunge(player);
            return;
        }
        // Let normal movement packets and Minecraft collision handling move the player smoothly.
        player.setVelocity(event.getVector());
        player.setFallDistance(0);
        lungeTrail(player);
    }

    /**
     * Sweeps the player's hitbox toward the target in ~0.1 block steps looking for a wall. The test box ramps
     * up to a higher target so leaping onto a ledge reads as clear, but never dips below standing height for a
     * lower one - a downhill lunge clears the ground horizontally then falls under its own velocity, and
     * following the diagonal would clip the box through the ledge it is leaping off.
     */
    private boolean clearPath(Player player, Vector velocity, double targetFeetY) {
        double distance = velocity.length();
        if (!Double.isFinite(distance) || distance > 4) return false;
        final Location start = player.getLocation();
        final BoundingBox box = player.getBoundingBox();
        final double startFeetY = start.getY();
        final int steps = Math.max(1, (int) Math.ceil(distance / 0.1));
        for (int step = 1; step <= steps; step++) {
            final double t = (double) step / steps;
            final double dx = velocity.getX() * t;
            final double dz = velocity.getZ() * t;
            final double dy = Math.max(0.0, (targetFeetY - startFeetY) * t); // ramp up to a higher target; never dip below
            if (blocked(start.clone().add(dx, dy, dz), box.clone().shift(dx, dy, dz))) return false;
        }
        return true;
    }

    private boolean blocked(Location location, BoundingBox box) {
        return blocked(location, box, true);
    }

    private boolean blocked(Location location, BoundingBox box, boolean allowStep) {
        if (!location.getWorld().getWorldBorder().isInside(location)) return true;
        final double stepTop = box.getMinY() + 0.6; // anything topping out this low, the player just walks up
        for (int x = (int) Math.floor(box.getMinX()); x <= Math.floor(box.getMaxX()); x++) {
            for (int y = (int) Math.floor(box.getMinY()); y <= Math.floor(box.getMaxY()); y++) {
                if (y < location.getWorld().getMinHeight() || y >= location.getWorld().getMaxHeight()) return true;
                for (int z = (int) Math.floor(box.getMinZ()); z <= Math.floor(box.getMaxZ()); z++) {
                    if (!location.getWorld().isChunkLoaded(x >> 4, z >> 4)) return true;
                    for (BoundingBox obstacle : UtilBlock.getBoundingBoxes(location.getWorld().getBlockAt(x, y, z))) {
                        if (allowStep && obstacle.getMaxY() <= stepTop) continue; // low step, not a wall
                        if (obstacle.overlaps(box)) return true;
                    }
                }
            }
        }
        return false;
    }

    private void finishLunge(Player player, Lunge lunge) {
        snapLungeArrival(player, lunge);
        stopLunge(player);
        player.setFallDistance(0);
        final double connect = lungeStopDistance + 2.0; // reach check only; snapLungeArrival governs spacing
        if (validTarget(player, lunge.target()) && player.hasLineOfSight(lunge.target())
                && player.getLocation().distanceSquared(lunge.target().getLocation()) <= connect * connect) {
            // Stuck the dive - start the hunt: a countdown to secure the kill and unlock the escape.
            final Hunt hunt = new Hunt(lunge.target(), lunge.anchor(),
                    System.currentTimeMillis() + (long) (huntDuration * 1000));
            hunts.put(player, hunt);
            showHuntBar(player, hunt);
            playLungeHit(lunge.target().getLocation());
        }
    }

    /** Correct residual motion and target movement without snapping into or through blocks. */
    private void snapLungeArrival(Player player, Lunge lunge) {
        final Location start = player.getLocation();
        final Location target = lunge.target().getLocation();
        Vector away = start.toVector().subtract(target.toVector());
        if (away.lengthSquared() < 1.0e-6) {
            // Exact overlap has no direction; use the side from which the hunter committed the dive.
            away = lunge.anchor().toVector().subtract(target.toVector());
            if (away.lengthSquared() < 1.0e-6) {
                away = start.getDirection().setY(0).multiply(-1);
                if (away.lengthSquared() < 1.0e-6) away = new Vector(1, 0, 0);
            }
        }
        final Location arrival = target.clone().add(away.normalize().multiply(lungeStopDistance));
        arrival.setYaw(start.getYaw());
        arrival.setPitch(start.getPitch());
        final Vector correction = arrival.toVector().subtract(start.toVector());
        if (!clearPath(player, correction, arrival.getY())) return;

        // Unlike ordinary movement, a teleport cannot step up out of a partial floor intersection.
        // Sweep the actual correction as well as the ledge-aware movement path above.
        final BoundingBox box = player.getBoundingBox();
        final int steps = Math.max(1, (int) Math.ceil(correction.length() / 0.1));
        for (int step = 1; step <= steps; step++) {
            final Vector offset = correction.clone().multiply((double) step / steps);
            if (blocked(start.clone().add(offset), box.clone().shift(offset), false)) return;
        }
        player.setVelocity(new Vector());
        lungeArrivals.put(player, arrival);
        try {
            player.teleport(arrival);
        } finally {
            lungeArrivals.remove(player);
        }
    }

    /** Stops an in-flight lunge that failed to reach its target, with a short "missed" cue. */
    private void fizzleLunge(Player player) {
        if (!lunges.containsKey(player)) return;
        stopLunge(player);
        if (!player.isOnline() || getLevel(player) <= 0) return; // unequipped mid-lunge - ownership loss, no cooldown
        startCooldown(player); // the dive is spent, even if it hit a wall
        new SoundEffect(Sound.ENTITY_ITEM_BREAK, 0.7f, 0.9f).play(player.getLocation());
        lungeSpark(player.getLocation().add(0, 1, 0), 8, 0.25);
    }

    private void stopLunge(Player player) {
        if (lunges.remove(player) != null && player.isOnline()) {
            player.setVelocity(new Vector());
        }
    }

    /** The marked target dies while the hunt is live: open the shadowstep window for the hunter. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMarkedKill(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player victim)) return;
        for (Player hunter : List.copyOf(hunts.keySet())) {
            final Hunt hunt = hunts.get(hunter);
            if (hunt == null || !victim.equals(hunt.target())) continue;
            hunts.remove(hunter);
            if (!hunter.isOnline() || getLevel(hunter) <= 0) continue; // ownership loss - no cooldown
            if (System.currentTimeMillis() >= hunt.expires()
                    || !hunter.getWorld().equals(hunt.anchor().getWorld())) {
                startCooldown(hunter); // combo is over but no escape - too slow, or the anchor is a world away
                continue;
            }
            final Shadowstep step = new Shadowstep(hunt.anchor(),
                    System.currentTimeMillis() + (long) (returnWindow * 1000));
            shadowsteps.put(hunter, step);
            showShadowstepBar(hunter, step);
            playShadowstepOffered(hunter);
            UtilMessage.message(hunter, getClassType().getDisplayName(),
                    "champions.skill.assassin.predators-mark.shadowstep-ready");
        }
    }

    /** Starts the escape dash back to the leap point. A wall stops it short; too far from the anchor and it just fails. */
    private void performShadowstep(Player player, Shadowstep step) {
        startCooldown(player); // the escape is spent whether the dash lands or not
        final Location anchor = step.anchor().clone();
        if (!player.getWorld().equals(anchor.getWorld())
                || player.getLocation().distanceSquared(anchor) > returnMaxDistance * returnMaxDistance) {
            message(player, "shadowstep-failed"); // dragged too far from the leap point to dash back
            return;
        }
        final ReturnDash dash = new ReturnDash(anchor,
                System.currentTimeMillis() + (long) (maxLungeDuration * 1000));
        returnDashes.put(player, dash);
        shadowstepPuff(player.getLocation());
        playLungeStart(player);
        advanceReturn(player, dash);
    }

    private void advanceReturn(Player player, ReturnDash dash) {
        if (!isEnabled() || getLevel(player) <= 0 || !player.getWorld().equals(dash.anchor().getWorld())
                || System.currentTimeMillis() >= dash.expires()) {
            fizzleReturn(player);
            return;
        }
        final Location start = player.getLocation();
        final Vector delta = dash.anchor().toVector().subtract(start.toVector());
        final double distance = delta.length();
        if (distance <= 1.5) {
            finishReturn(player);
            return;
        }
        final Vector velocity = delta.multiply(Math.min(lungeSpeed, distance) / distance);
        final CustomEntityVelocityEvent event = UtilServer.callEvent(
                new CustomEntityVelocityEvent(player, null, VelocityType.CUSTOM, velocity));
        if (event.isCancelled() || !clearPath(player, event.getVector(), dash.anchor().getY())) {
            fizzleReturn(player);
            return;
        }
        player.setVelocity(event.getVector());
        player.setFallDistance(0);
        lungeTrail(player);
    }

    /** The dash reached the leap point. */
    private void finishReturn(Player player) {
        if (returnDashes.remove(player) == null) return;
        if (!player.isOnline()) return;
        player.setVelocity(new Vector());
        player.setFallDistance(0);
        shadowstepPuff(player.getLocation());
    }

    /** A wall cut the dash short - the hunter stops where it hit. */
    private void fizzleReturn(Player player) {
        if (returnDashes.remove(player) == null) return;
        if (!player.isOnline()) return;
        player.setVelocity(new Vector());
        new SoundEffect(Sound.ENTITY_ITEM_BREAK, 0.7f, 0.9f).play(player.getLocation());
        lungeSpark(player.getLocation().add(0, 1, 0), 8, 0.25);
    }

    private void stopReturn(Player player) {
        if (returnDashes.remove(player) != null && player.isOnline()) {
            player.setVelocity(new Vector());
        }
    }

    private void message(Player player, String key) {
        UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.predators-mark." + key);
    }

    /**
     * Commits the ability cooldown. Called by hand at every point the sequence can end - a mark that lapses,
     * a fizzled lunge, a hunt that times out, an escape window missed, or the escape being taken - so the 20s
     * never ticks while the player is still mid-combo. No-ops if a cooldown is already running.
     */
    private void startCooldown(Player player) {
        if (championsManager.getCooldowns().hasCooldown(player, getName())) return;
        championsManager.getCooldowns().use(player, getName(), getCooldown(getLevel(player)), showCooldownFinished(),
                true, isCancellable(), this::shouldDisplayActionBar, getPriority());
    }

    /**
     * Ends a placed mark. {@code applyCooldown} commits the cooldown now - pass true when the mark simply lapses
     * or loses its target, false when something else will own it: the lunge about to start, or an ownership loss
     * (build change, skill disabled, disconnect, shutdown).
     */
    private void endMark(Player player, boolean applyCooldown) {
        updateVignette(player, null);
        Mark mark = marks.remove(player);
        if (mark == null) return;
        clearGlow(player, mark); // the lunge-window bar clears itself once the mark leaves the map
        if (applyCooldown) {
            startCooldown(player);
        }
    }

    private void showLungeWindowBar(Player player, Mark mark) {
        showTimerBar(player, "Lunge", markDuration, () -> marks.get(player) == mark ? mark.expires : null);
    }

    private void showHuntBar(Player player, Hunt hunt) {
        showTimerBar(player, "Hunt", huntDuration, () -> hunts.get(player) == hunt ? hunt.expires() : null);
    }

    private void showShadowstepBar(Player player, Shadowstep step) {
        showTimerBar(player, "Shadowstep", returnWindow, () -> shadowsteps.get(player) == step ? step.expires() : null);
    }

    /**
     * A self-managed action-bar countdown drawn like a live cooldown bar - bold white label, green-fill bar,
     * white seconds. Sits just below the ability cooldown's priority (lower wins) so it shows over the
     * "Predators Mark" recharge running underneath. Not a real cooldown - that would leave a stale "recharged"
     * bar afterwards. The provider returns null once its state is gone, ending the bar at once.
     */
    private void showTimerBar(Player player, String label, double window, Supplier<Long> expiresAt) {
        final Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().add(getPriority() - 1, new TimedComponent(window + 1, false, viewer -> {
            final Long expires = expiresAt.get();
            if (expires == null || !shouldDisplayActionBar(viewer)) return null;
            final double remaining = (expires - System.currentTimeMillis()) / 1000.0;
            if (remaining <= 0) return null;
            final float progress = (float) Math.max(0, Math.min(1, 1 - remaining / window));
            return Component.join(JoinConfiguration.separator(Component.space()),
                    Component.text(label, NamedTextColor.WHITE, TextDecoration.BOLD),
                    ProgressBar.withProgress(progress).build(),
                    Component.text(String.format("%.1fs", remaining), NamedTextColor.WHITE));
        }));
    }

    // --- Presentation -------------------------------------------------------------------------------------

    /** The moment a mark lands: a lock-on cue for the hunter, a heartbeat for the prey. */
    private void playMarkApplied(Player caster, Player target) {
        new SoundEffect(Sound.ENTITY_ARROW_HIT_PLAYER, 1.6f, 0.8f).play(caster);
        new SoundEffect(Sound.ENTITY_WOLF_GROWL, 0.7f, 0.5f).play(caster);
        new SoundEffect(Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 1.0f).play(target);

        final Location center = target.getLocation();
        for (int i = 0; i < 18; i++) {
            final double angle = Math.PI * 2 * i / 18;
            final Location point = center.clone().add(Math.cos(angle) * 0.95, 0.15, Math.sin(angle) * 0.95);
            Particle.DUST.builder().location(point).count(1).extra(0)
                    .data(new Particle.DustOptions(Color.fromRGB(150, 12, 12), 1.4f))
                    .receivers(caster).spawn();
        }
    }

    /** Soft descending tone for the hunter when a mark ends without a lunge. */
    private void markLostCue(Player caster) {
        if (!caster.isOnline()) return;
        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f).play(caster);
    }

    /** Hunter-only reticle bobbing over the marked target while the lunge window is open. */
    private void pulseReticle(Player caster, Mark mark, long now) {
        if (now < mark.nextPulse) return;
        mark.nextPulse = now + 250;
        final Location head = mark.target.getLocation().add(0, 2.35, 0);
        Particle.DUST.builder().location(head).count(5).offset(0.12, 0.10, 0.12).extra(0)
                .data(new Particle.DustOptions(Color.fromRGB(185, 28, 28), 1.1f))
                .receivers(caster).spawn();
    }

    /** The skill's one particle - a violet puff, reused at every beat. Dust only; nothing that blocks the view. */
    private void lungeSpark(Location at, int count, double spread) {
        Particle.DUST.builder().location(at).count(count).offset(spread, spread, spread).extra(0)
                .data(new Particle.DustOptions(Color.fromRGB(122, 40, 190), 1.3f)).receivers(30).spawn();
    }

    private void playLungeStart(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.5f);
        new SoundEffect(Sound.ENTITY_PHANTOM_FLAP, 0.7f, 0.7f).play(player.getLocation());
        lungeSpark(player.getLocation().add(0, 1, 0), 12, 0.25);
    }

    private void lungeTrail(Player player) {
        lungeSpark(player.getLocation().add(0, 1, 0), 3, 0.15);
    }

    private void playLungeHit(Location target) {
        final Location at = target.clone().add(0, 1, 0);
        new SoundEffect(Sound.ENTITY_PHANTOM_BITE, 0.8f, 0.8f).play(at);
        lungeSpark(at, 12, 0.3);
    }

    /** Private cue to the hunter that the shadowstep window is open. */
    private void playShadowstepOffered(Player player) {
        new SoundEffect(Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.6f, 0.6f).play(player);
        new SoundEffect(Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 0.6f).play(player);
    }

    /** The violet burst at each end of a shadowstep, low at the hunter's feet and identical at both ends. */
    private void shadowstepPuff(Location at) {
        new SoundEffect(Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.7f).play(at);
        lungeSpark(at.clone().add(0, 0.3, 0), 24, 0.45);
    }

    // -----------------------------------------------------------------------------------------------------

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        equippedPlayers.add(player);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        endMark(player, false);
        stopLunge(player);
        stopReturn(player);
        hunts.remove(player);
        shadowsteps.remove(player);
        equippedPlayers.remove(player);
    }

    private void removePlayer(Player player) {
        // Death and teleport do not unequip the skill; keep preview tracking for the next valid tick.
        // Abandoning a live combo by self-teleport or quitting still commits the cooldown (death resets it anyway).
        if (!player.isDead() && (hunts.containsKey(player) || shadowsteps.containsKey(player)
                || returnDashes.containsKey(player))) {
            startCooldown(player);
        }
        endMark(player, false);
        stopLunge(player);
        stopReturn(player);
        hunts.remove(player);
        shadowsteps.remove(player);
        for (Player owner : List.copyOf(marks.keySet())) {
            if (marks.get(owner).target.equals(player)) {
                markLostCue(owner);
                endMark(owner, true); // the target they committed to has left - the cooldown stands
            }
        }
        for (Player owner : List.copyOf(lunges.keySet())) {
            if (lunges.get(owner).target().equals(player)) stopLunge(owner);
        }
        for (Player owner : List.copyOf(vignetteTargets.keySet())) {
            if (vignetteTargets.get(owner).equals(player)) updateVignette(owner, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo().equals(lungeArrivals.get(event.getPlayer()))) return;
        // Covers world changes and external teleports without steering across a teleport.
        removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer());
        equippedPlayers.remove(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        removePlayer(event.getEntity());
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != champions) return;
        List.copyOf(marks.keySet()).forEach(player -> endMark(player, false));
        List.copyOf(lunges.keySet()).forEach(this::stopLunge);
        List.copyOf(returnDashes.keySet()).forEach(this::stopReturn);
        List.copyOf(vignetteTargets.keySet()).forEach(player -> updateVignette(player, null));
        hunts.clear();
        shadowsteps.clear();
        equippedPlayers.clear();
    }

    @Override
    public void loadSkillConfig() {
        range = Math.max(0.1, getConfig("range", 20.0, Number.class).doubleValue());
        markDuration = Math.max(0.1, getConfig("markDuration", 7.0, Number.class).doubleValue());
        blindDuration = Math.max(0, getConfig("blindDuration", 1.5, Number.class).doubleValue());
        lungeSpeed = Math.max(0.1, Math.min(3, getConfig("lungeSpeed", 1.5, Number.class).doubleValue()));
        lungeStopDistance = Math.max(0, getConfig("lungeStopDistance", 3.0, Number.class).doubleValue());
        maxLungeDuration = Math.max(0.1, getConfig("maxLungeDuration", 2.0, Number.class).doubleValue());
        huntDuration = Math.max(0.5, getConfig("huntDuration", 8.0, Number.class).doubleValue());
        returnWindow = Math.max(0.5, getConfig("returnWindow", 4.0, Number.class).doubleValue());
        returnMaxDistance = Math.max(1, getConfig("returnMaxDistance", 40.0, Number.class).doubleValue());
    }

    /** A placed mark: the glowing target and the deadline to re-drop and lunge. */
    private static final class Mark {
        private final Player target;
        private final long expires;
        private boolean glowing;
        private boolean glowFailed;
        private long nextPulse;

        private Mark(Player target, long expires) {
            this.target = target;
            this.expires = expires;
        }
    }

    /** A lunge in flight toward its target. {@code world} guards against the target changing dimensions mid-air. */
    private record Lunge(Player target, World world, Location anchor, long expires) { }

    /** A stuck lunge: killing {@code target} before {@code expires} earns the shadowstep back to {@code anchor}. */
    private record Hunt(Player target, Location anchor, long expires) { }

    /** The open escape window - drop again before {@code expires} to dash back to {@code anchor}. */
    private record Shadowstep(Location anchor, long expires) { }

    /** The escape dash itself, in flight toward {@code anchor} until it arrives or {@code expires}. */
    private record ReturnDash(Location anchor, long expires) { }
}
