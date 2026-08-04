package me.mykindos.betterpvp.clans.world.discovery.sighting;

import lombok.Getter;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import me.mykindos.betterpvp.clans.world.island.IslandOffer;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * An island on the horizon, and the two displays that put it there.
 * <p>
 * The offer is drawn when the sighting appears rather than when the crew gets there, so what is on the horizon is a
 * particular place from the moment it is seen: its colour is its own, and steering for it lands the crew on the island
 * they were looking at.
 * <p>
 * The icon is pinned at a fixed range on its true bearing — see {@link SightingGeometry} — so only the distance text
 * beneath it and the last stretch of growth say anything about closing.
 */
public class Sighting {

    /** How big the icon is drawn from the ramp start outward. */
    private static final float MIN_SCALE = 10f;

    /** How big it has grown by the time the crew arrives. */
    private static final float MAX_SCALE = 26f;

    /** Blocks above the water the icon sits, so it reads as land on the horizon rather than as flotsam. */
    private static final double ICON_LIFT = 14.0;

    /** Blocks of clearance between the icon's crown and the range beneath — the icon grows, so this rides on top of it. */
    private static final double LABEL_GAP = 2.0;

    /** The range has to carry from the horizon, so it is drawn far larger than a nameplate would be. */
    private static final float LABEL_SCALE = 5f;

    /** Both displays are pinned further out than a display carries by default, and would otherwise never be sent. */
    private static final float VIEW_RANGE_BLOCKS = 250f;

    /** Below this a rescale is not worth the packet: the icon grows over a hundred blocks of sailing. */
    private static final float SCALE_EPSILON = 0.25f;

    @Getter
    private final OceanPoint point;

    /** The island this is: allocated only if the crew actually reaches it. */
    @Getter
    private final IslandOffer offer;

    private final Color glow;

    private Display icon;
    private TextDisplay label;
    private float scale;

    public Sighting(@NotNull OceanPoint point, @NotNull IslandOffer offer) {
        this.point = point;
        this.offer = offer;
        this.glow = SightingPalette.of(offer.getTemplate().getKey());
    }

    /**
     * Draws it on the bearing it currently lies on.
     *
     * @param at            where the pinned bearing falls in the world
     * @param trueDistance  how far the island actually is, which is what the crew is told
     * @param refreshLabel  whether the distance text is due an update; it changes slowly and a per-tick text packet per
     *                      sailor buys nothing
     */
    public void render(@NotNull Location at, double trueDistance, double arrivalDistance, double rampStartDistance,
                       boolean refreshLabel) {
        final World world = at.getWorld();
        if (world == null) {
            return;
        }

        final Location iconAt = at.clone().add(0, ICON_LIFT, 0);

        // A display the server took out from under us leaves a handle that teleports forever without ever drawing.
        if (icon != null && !icon.isValid()) {
            remove();
        }

        if (icon == null) {
            icon = spawnIcon(iconAt);
            label = spawnLabel(iconAt);
        }

        applyScale(SightingGeometry.scale(trueDistance, arrivalDistance, rampStartDistance, MIN_SCALE, MAX_SCALE));

        icon.teleport(iconAt);
        // The icon is centred on its anchor, so its crown rises by half of whatever it has grown to.
        label.teleport(at.clone().add(0, ICON_LIFT + scale / 2.0 + LABEL_GAP, 0));
        if (refreshLabel) {
            label.text(Translations.component("clans.discovery.sighting.range",
                    Component.text(Math.round(trueDistance))));
        }
    }

    /** Takes both displays back out of the world. */
    public void remove() {
        if (icon != null) {
            icon.remove();
            icon = null;
        }
        if (label != null) {
            label.remove();
            label = null;
        }
    }

    /**
     * The land itself. A block of grass stands in for the island until there is a font icon to put there — swapping it
     * is a matter of spawning a different {@link Display} here, since nothing else touches what it is made of.
     */
    protected @NotNull Display spawnIcon(@NotNull Location at) {
        return at.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(Material.GRASS_BLOCK.createBlockData());
            dress(display);
        });
    }

    private @NotNull TextDisplay spawnLabel(@NotNull Location at) {
        return at.getWorld().spawn(at, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setSeeThrough(true);
            display.setShadowed(true);
            display.text(Component.empty());
            display.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(), new Vector3f(LABEL_SCALE), new AxisAngle4f()));
            dress(display);
        });
    }

    /**
     * What both displays need to survive being repositioned every tick at the edge of the crew's render distance:
     * without the teleport duration they strobe between ticks, and without the view range they are simply never sent.
     */
    private void dress(@NotNull Display display) {
        display.setPersistent(false);
        display.setTeleportDuration(1);
        display.setGlowing(true);
        display.setGlowColorOverride(glow);
        UtilEntity.setViewRangeBlocks(display, VIEW_RANGE_BLOCKS);
    }

    private void applyScale(float target) {
        if (Math.abs(target - scale) < SCALE_EPSILON) {
            return;
        }
        scale = target;

        // Centred on its own anchor, so growing does not walk the icon off its bearing.
        icon.setTransformation(new Transformation(
                new Vector3f(-target / 2f, -target / 2f, -target / 2f),
                new AxisAngle4f(),
                new Vector3f(target),
                new AxisAngle4f()));
    }
}
