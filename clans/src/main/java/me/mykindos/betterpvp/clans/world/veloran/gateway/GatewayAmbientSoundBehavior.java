package me.mykindos.betterpvp.clans.world.veloran.gateway;

import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * The Sundered Gate's atmospheric bed: an unsettling soul-sand-valley drone anchored at the portal's centre, with
 * occasional stinger "additions" layered over it.
 * <p>
 * The looping track is re-played just before it runs out so it never gaps, while the additions fire on a slower beat to
 * keep the ambience from feeling static. Like the comet behaviour, all work is skipped when nobody is near enough to
 * hear it.
 */
public class GatewayAmbientSoundBehavior implements SceneBehavior {

    /** Don't emit unless a player is within this radius of the portal centre. */
    private static final int HEARING_RANGE = 120;

    private final CuboidRegion portalRegion;
    private final float hummingPitch;

    private int age = 0;

    public GatewayAmbientSoundBehavior(CuboidRegion portalRegion, float hummingPitch) {
        this.portalRegion = portalRegion;
        this.hummingPitch = hummingPitch;
    }

    @Override
    public void tick() {
        final World world = portalRegion.getWorld();
        if (world == null) {
            return;
        }

        final Location min = portalRegion.getMin();
        final Location max = portalRegion.getMax();
        final Location loc = new Vector(
                (min.getX() + max.getX()) / 2.0,
                (min.getY() + max.getY()) / 2.0,
                (min.getZ() + max.getZ()) / 2.0).toLocation(world);

        final List<Player> nearby = world.getPlayers().stream()
                .filter(player -> player.getLocation().distanceSquared(loc) <= HEARING_RANGE * HEARING_RANGE)
                .toList();
        final boolean anyoneListening = !nearby.isEmpty();
        if (!anyoneListening) {
            age = 0; // reset so the drone restarts cleanly the moment someone returns
            return;
        }

        final ForwardingAudience audience = Audience.audience(nearby);
        if (age % 120 == 0) {
            new SoundEffect(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, hummingPitch, 0.8f).play(audience);
        } else if (age % 60 == 0) {
            new SoundEffect(Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, hummingPitch, 1.2f).play(audience);
        }

        if (age % 160 == 0 && Math.random() < 0.5) {
            String sound = "raid.ambient_" + UtilMath.randomInt(7);
            new SoundEffect("betterpvp", sound, 1f, 0.1f).play(audience);
        }

        age++;
    }
}
