package me.mykindos.betterpvp.core.world.construction.view;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostMesher;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPiece;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The part of a finished structure still waiting to be claimed, drawn as glowing blocks everyone can see, blinking so
 * the structure stands out as ready.
 */
final class ClaimFlash {

    static final Color COLOUR = Color.fromRGB(0xFFD24D);

    private final List<BlockDisplay> displays = new ArrayList<>();
    private boolean lit = true;

    /** @param blocks the held-back blocks, at their world positions */
    ClaimFlash(@NotNull World world, @NotNull List<Schematic.PlacedBlock> blocks, @NotNull GhostMesher mesher) {
        for (GhostPiece piece : mesher.mesh(blocks)) {
            final Location at = new Location(world, piece.getX(), piece.getY(), piece.getZ());
            displays.add(world.spawn(at, BlockDisplay.class, spawned -> {
                spawned.setPersistent(false);
                spawned.setBlock(piece.getData());
                spawned.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(piece.getWidth(), piece.getHeight(), piece.getDepth()), new AxisAngle4f()));
                spawned.setBrightness(new Display.Brightness(15, 15));
                spawned.setGlowing(true);
                spawned.setGlowColorOverride(COLOUR);
            }));
        }
    }

    void blink() {
        lit = !lit;
        displays.forEach(display -> display.setGlowing(lit));
    }

    void remove() {
        displays.forEach(BlockDisplay::remove);
        displays.clear();
    }
}
