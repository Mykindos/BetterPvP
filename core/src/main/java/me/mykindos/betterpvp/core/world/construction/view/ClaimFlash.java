package me.mykindos.betterpvp.core.world.construction.view;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostShell;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A finished structure waiting to be claimed, drawn over as glowing blocks everyone can see, blinking so it stands out
 * as ready.
 */
final class ClaimFlash {

    static final Color COLOUR = Color.fromRGB(0xFFD24D);

    private final List<BlockDisplay> displays = new ArrayList<>();
    private boolean lit = true;

    /** @param blocks the structure's blocks, at their world positions */
    ClaimFlash(@NotNull World world, @NotNull List<Schematic.PlacedBlock> blocks, @NotNull GhostShell shell) {
        for (Schematic.PlacedBlock block : shell.visible(blocks)) {
            final Location at = new Location(world, block.getX(), block.getY(), block.getZ());
            displays.add(world.spawn(at, BlockDisplay.class, spawned -> {
                spawned.setPersistent(false);
                spawned.setBlock(block.getData());
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
