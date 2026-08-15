package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A running copy of the caster, moved by this class rather than by the server's entity physics.
 * <p>
 * The body is a {@link Mannequin} wearing the caster's profile and gear, spawned immovable and without gravity so that
 * nothing but {@link #tick(float, double, double)} ever changes where it is. Owning the motion is what makes this decoy
 * possible at all: a pathfinding mob needs a destination, and an illusion only ever has a heading - the caster's yaw,
 * which can swing to a new one at any moment.
 * <p>
 * The step reproduces vanilla player movement closely enough to be mistaken for one. Vertical motion uses the same
 * constants and the same integration order the server applies to a real player, so the decoy's jump arc and fall speed
 * match a player's exactly; it kicks up the same sprint particles a player does; and it hops when the step ahead is
 * blocked but the one above it is clear, which is the input a player gives to climb a hill.
 */
class IllusionDecoy {

    private final Mannequin body;
    private final Location location;
    private final long expiryTime;

    /** Viewer UUID to the scoreboard team their client has the caster on, so the plate can be taken off it again. */
    private final Map<UUID, String> nameplateTeams = new HashMap<>();

    /** The skill level this illusion was cast at, so its ending is scored against the cast rather than the build now. */
    @Getter private final int level;

    private final double health;
    private final EntityHealthService healthService;
    private boolean healthSettled;

    /** Vertical speed in blocks per tick. Negative while falling, zero while stood on something. */
    private double verticalVelocity;
    private boolean grounded;

    IllusionDecoy(@NotNull Player caster, int level, double health, @NotNull EntityHealthService healthService,
                  long durationMillis) {
        this.level = level;
        this.health = health;
        this.healthService = healthService;
        this.expiryTime = System.currentTimeMillis() + durationMillis;
        this.location = caster.getLocation().clone();
        this.location.setPitch(0f);

        this.body = caster.getWorld().spawn(location, Mannequin.class, mannequin -> {
            mannequin.setProfile(ResolvableProfile.resolvableProfile(caster.getPlayerProfile()));
            mannequin.setDescription(null);
            mannequin.customName(Component.text(caster.getName()));
            mannequin.setCustomNameVisible(true);
            mannequin.setImmovable(true);
            mannequin.setGravity(false);
            mannequin.setPersistent(false);
            mannequin.setCollidable(false);
            mannequin.setAI(false);
            wear(caster, mannequin.getEquipment());
        });

        // Pick the illusion up mid-stride: cast off a jump and it leaves the ground already climbing, on the same arc
        // the caster is on. Only the vertical carries over - the horizontal is the decoy's own fixed sprint.
        this.verticalVelocity = caster.getVelocity().getY();
        body.setVelocity(caster.getVelocity());
        this.grounded = false;

        healthService.setBaseHealth(body, health);
        body.setHealth(health);
        joinCasterTeam(caster);
    }

    /**
     * Holds the decoy at its configured health.
     * <p>
     * Re-checked each tick rather than set once, because dressing the body hands it a champions role a tick later and
     * {@code RoleManager} writes the role's own health over whatever was here - after firing {@code RoleChangeEvent},
     * so there is no event to listen for that lands late enough. In practice this corrects exactly once, on the tick
     * the role arrives, which is why the first correction is also allowed to top the body back up.
     */
    private void enforceHealth() {
        final AttributeInstance maxHealth = body.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getBaseValue() == health) {
            return;
        }

        healthService.setBaseHealth(body, health);
        body.setHealth(healthSettled ? Math.min(body.getHealth(), health) : health);
        healthSettled = true;
    }

    /**
     * Dresses the body in the caster's own gear, so it is armoured and armed for real - it takes hits like the kit it
     * appears to be wearing, and makes the kit's sounds, without this class knowing anything about kits.
     * <p>
     * Every setter takes its silent overload. That is the only thing being suppressed: the per-piece equip sound of
     * dressing a body that is meant to have been wearing this all along. Everything downstream of the equipment still
     * runs, which is the point - {@code RoleArmorListener} picks the change up and hands the body the matching role,
     * and with it the role's health, so kit changes reach the illusion without a second copy of the kit living here.
     */
    private static void wear(@NotNull Player caster, @NotNull EntityEquipment gear) {
        final PlayerInventory worn = caster.getInventory();
        gear.setHelmet(worn.getHelmet(), true);
        gear.setChestplate(worn.getChestplate(), true);
        gear.setLeggings(worn.getLeggings(), true);
        gear.setBoots(worn.getBoots(), true);
        gear.setItemInMainHand(worn.getItemInMainHand(), true);
        gear.setItemInOffHand(worn.getItemInOffHand(), true);
    }

    /** Whether {@code entity} is this decoy's body, for matching a damage or death event back to its owner. */
    boolean isBody(@NotNull Entity entity) {
        return body.equals(entity);
    }

    boolean isExpired() {
        return System.currentTimeMillis() >= expiryTime;
    }

    @NotNull Location getLocation() {
        return location.clone();
    }

    /**
     * Advances the decoy one tick.
     *
     * @param targetYaw the heading to run along, normally the caster's current yaw
     * @param turnRate  the most the heading may change this tick, in degrees
     * @param speed     forward speed in blocks per tick
     */
    void tick(float targetYaw, double turnRate, double speed) {
        location.setYaw(approachYaw(location.getYaw(), targetYaw, turnRate));

        final Vector step = location.getDirection().setY(0);
        if (step.lengthSquared() > .0E-6) {
            step.normalize().multiply(speed);
            final Location ahead = location.clone().add(step);
            if (canOccupy(ahead)) {
                location.setX(ahead.getX());
                location.setZ(ahead.getZ());
                if (grounded) {
                    spawnSprintParticle(step);
                }
            } else if (grounded && canOccupy(ahead.add(0, 1, 0))) {
                // Blocked at foot height with headroom one block up: the same wall a player clears by jumping into it.
                // 0.42 is the vanilla jump_strength attribute default, which carries a player 1.2522 blocks up.
                verticalVelocity = 0.42;
            }
        }

        applyGravity();
        enforceHealth();
        body.teleport(location);
    }

    /**
     * Falls, lands, and bumps its head.
     * <p>
     * Vanilla integrates a player's vertical motion as {@code y += vy} and only then {@code vy = (vy - g) * drag}, with
     * {@code g} the 0.08 gravity attribute default and {@code drag} the 0.98 from {@code LivingEntity#travel}. Applying
     * the impulse before decaying it is what gives a jump its full first tick - decay it first and the arc tops out
     * around 0.90 blocks, short of the one-block step it is supposed to clear. The -3.92 floor is the terminal velocity
     * those two constants settle at.
     */
    private void applyGravity() {
        final double nextY = location.getY() + verticalVelocity;

        if (verticalVelocity <= 0) {
            final double landing = landingHeight(nextY);
            grounded = !Double.isNaN(landing);
            location.setY(grounded ? landing : nextY);
            if (grounded) {
                verticalVelocity = 0;
                return;
            }
        } else {
            grounded = false;
            if (!canOccupy(location.clone().add(0, verticalVelocity, 0))) {
                verticalVelocity = 0;
                return;
            }
            location.setY(nextY);
        }

        verticalVelocity = Math.max((verticalVelocity - 0.08) * 0.98, -3.92);
    }

    /**
     * The surface the decoy comes to rest on somewhere between its current height and {@code targetY}, or
     * {@link Double#NaN} if that whole span is open air and it keeps falling.
     * <p>
     * Block bounding boxes are measured rather than assumed, so slabs and stairs are stood on at their real height
     * instead of the top of the block they sit in.
     */
    private double landingHeight(double targetY) {
        for (int y = location.getBlockY(); y >= (int) Math.floor(targetY); y--) {
            final Block block = location.getWorld().getBlockAt(location.getBlockX(), y, location.getBlockZ());
            if (!UtilBlock.solid(block)) {
                continue;
            }
            final double surface = block.getBoundingBox().getMaxY();
            if (surface <= location.getY() + 1.0E-6 && surface >= targetY) {
                return surface;
            }
        }
        return Double.NaN;
    }

    /**
     * Kicks up a scuff of the block underfoot, as {@code Entity#spawnSprintParticle} does for a real sprinting player:
     * one block particle per tick, offset randomly across the body's width, thrown backwards at four times the running
     * speed and upwards. A count of zero is what turns the offset into a velocity rather than a spread.
     */
    private void spawnSprintParticle(@NotNull Vector step) {
        final Block ground = location.clone().subtract(0, 0.2, 0).getBlock();
        if (!UtilBlock.solid(ground)) {
            return;
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        Particle.BLOCK.builder()
                .data(ground.getBlockData())
                .location(location.clone().add((random.nextDouble() - 0.5) * 0.6, 0.1, (random.nextDouble() - 0.5) * 0.6))
                .receivers(40)
                .count(0)
                .offset(-step.getX() * 4, 1.5, -step.getZ() * 4)
                .extra(1)
                .spawn();
    }

    /** Whether a player-sized body could stand with its feet at {@code spot}. */
    private boolean canOccupy(@NotNull Location spot) {
        return !UtilBlock.solid(spot.getBlock()) && !UtilBlock.solid(spot.clone().add(0, 1, 0).getBlock());
    }

    /** Rotates {@code current} at most {@code maxStep} degrees towards {@code target}, taking the shorter way round. */
    private static float approachYaw(float current, float target, double maxStep) {
        final float delta = Location.normalizeYaw(target - current);
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return current + (float) Math.copySign(maxStep, delta);
    }

    /**
     * Puts the body on whichever scoreboard team each viewer already has the caster on, so the plate above it inherits
     * that viewer's colour and clan prefix for the real player instead of a bare white name.
     * <p>
     * It has to go out as a packet rather than through the scoreboard API for two reasons: non-player entities are
     * barred from server-side scoreboards by {@code allowNonPlayerEntitiesOnScoreboards}, and team membership here is
     * per-viewer anyway - every player holds their own scoreboard, coloured by their own relationship to the caster.
     * Reading the team back off the viewer rather than rebuilding it keeps this ignorant of how the plate is coloured.
     */
    private void joinCasterTeam(@NotNull Player caster) {
        for (Player viewer : caster.getWorld().getPlayers()) {
            final Team team = viewer.getScoreboard().getEntryTeam(caster.getName());
            if (team == null) {
                continue;
            }
            nameplateTeams.put(viewer.getUniqueId(), team.getName());
            sendTeamEntry(viewer, team.getName(), WrapperPlayServerTeams.TeamMode.ADD_ENTITIES);
        }
    }

    /**
     * A non-player entity is keyed on a scoreboard by its UUID, where a player is keyed by name - so this is what the
     * client matches against the team when it draws the plate.
     */
    private void sendTeamEntry(@NotNull Player viewer, @NotNull String team, @NotNull WrapperPlayServerTeams.TeamMode mode) {
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(
                new WrapperPlayServerTeams(team, mode, (WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
                        body.getUniqueId().toString()));
    }

    void remove() {
        for (Map.Entry<UUID, String> entry : nameplateTeams.entrySet()) {
            final Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null && viewer.isOnline()) {
                sendTeamEntry(viewer, entry.getValue(), WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES);
            }
        }
        nameplateTeams.clear();

        if (body.isValid()) {
            body.remove();
        }
    }
}
