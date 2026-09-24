package me.mykindos.betterpvp.core.world.schematic.ghost;

import lombok.Getter;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A see-where-it-goes copy of a structure that only one player can see, drawn from glowing block displays.
 * <p>
 * The glow says whether it fits: green where it can go, red where it cannot. It is moved and turned with
 * {@link #show}, which moves the existing displays when only the position changes and rebuilds them when the rotation
 * does. Each rotation's mesh is worked out once and kept.
 */
public final class GhostPreview {

    public static final Color VALID = Color.fromRGB(0x55FF55);
    public static final Color INVALID = Color.fromRGB(0xFF5555);

    private final Plugin plugin;
    @Getter
    private final Player viewer;
    @Getter
    private final Schematic schematic;
    private final GhostMesher mesher;

    private final Map<Integer, List<GhostPiece>> meshes = new HashMap<>();
    private final List<BlockDisplay> displays = new ArrayList<>();
    private List<GhostPiece> shown = List.of();

    @Getter
    private @Nullable Location anchor;
    @Getter
    private int quarterTurns = -1;
    @Getter
    private boolean valid = true;
    /** Colours each piece on its own when set, instead of the whole ghost by {@link #valid}. */
    private @Nullable Predicate<GhostPiece> fits;

    public GhostPreview(@NotNull Plugin plugin, @NotNull Player viewer, @NotNull Schematic schematic,
                        @NotNull GhostMesher mesher) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.schematic = schematic;
        this.mesher = mesher;
    }

    /** How many displays the ghost is drawn with at {@code quarterTurns}, for callers bounding entity counts. */
    public int pieceCount(int quarterTurns) {
        return mesh(quarterTurns).size();
    }

    /** Shows the ghost with its anchor block on {@code anchor}, turned {@code quarterTurns} times. */
    public void show(@NotNull Location anchor, int quarterTurns) {
        final Location block = anchor.toBlockLocation();
        final int turns = Math.floorMod(quarterTurns, 4);
        if (turns != this.quarterTurns || this.anchor == null || block.getWorld() != this.anchor.getWorld()) {
            despawn();
            this.anchor = block;
            this.quarterTurns = turns;
            spawn();
            return;
        }
        if (block.getBlockX() == this.anchor.getBlockX() && block.getBlockY() == this.anchor.getBlockY()
                && block.getBlockZ() == this.anchor.getBlockZ()) {
            return;
        }

        this.anchor = block;
        for (int i = 0; i < displays.size(); i++) {
            final GhostPiece piece = shown.get(i);
            displays.get(i).teleport(block.clone().add(piece.getX(), piece.getY(), piece.getZ()));
        }
    }

    /** Glows green if {@code valid}, red if not. */
    public void setValid(boolean valid) {
        if (this.valid == valid && fits == null) {
            return;
        }
        this.valid = valid;
        this.fits = null;
        recolor();
    }

    /**
     * Glows each piece green where {@code fits} says it can go and red where not, rather than the whole ghost one
     * colour. Pieces are in blocks from the anchor, already turned. Lasts until {@link #setValid} is called.
     */
    public void tint(@NotNull Predicate<GhostPiece> fits) {
        this.fits = fits;
        recolor();
    }

    /** Takes the ghost away. It can be shown again afterwards. */
    public void hide() {
        despawn();
        this.anchor = null;
        this.quarterTurns = -1;
    }

    private @NotNull List<GhostPiece> mesh(int quarterTurns) {
        return meshes.computeIfAbsent(quarterTurns, turns -> {
            // Placed on a world-less origin, so the blocks come out already turned and relative to the anchor.
            final List<Schematic.PlacedBlock> blocks = SchematicPlacement.of(schematic,
                    new Location(null, 0, 0, 0), turns).getBlocks();
            return List.copyOf(mesher.mesh(blocks));
        });
    }

    private void spawn() {
        shown = mesh(quarterTurns);
        for (GhostPiece piece : shown) {
            final Location at = anchor.clone().add(piece.getX(), piece.getY(), piece.getZ());
            final BlockDisplay display = at.getWorld().spawn(at, BlockDisplay.class, spawned -> {
                // Hidden before it is ever sent, so nobody but the viewer gets a single frame of it.
                spawned.setVisibleByDefault(false);
                spawned.setPersistent(false);
                spawned.setBlock(piece.getData());
                spawned.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(piece.getWidth(), piece.getHeight(), piece.getDepth()), new AxisAngle4f()));
                spawned.setBrightness(new Display.Brightness(15, 15));
                spawned.setTeleportDuration(2);
                spawned.setGlowing(true);
                spawned.setGlowColorOverride(color(piece));
            });
            viewer.showEntity(plugin, display);
            displays.add(display);
        }
    }

    private void despawn() {
        displays.forEach(BlockDisplay::remove);
        displays.clear();
        shown = List.of();
    }

    private void recolor() {
        for (int i = 0; i < displays.size(); i++) {
            displays.get(i).setGlowColorOverride(color(shown.get(i)));
        }
    }

    private @NotNull Color color(@NotNull GhostPiece piece) {
        final boolean green = fits == null ? valid : fits.test(piece);
        return green ? VALID : INVALID;
    }
}
