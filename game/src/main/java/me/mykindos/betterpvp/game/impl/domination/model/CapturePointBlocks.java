package me.mykindos.betterpvp.game.impl.domination.model;

import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;

public class CapturePointBlocks implements Lifecycled {

    private final CapturePoint point;
    private final Set<Block> capturingBlocks; // Wool blocks visible from the point (used for progressive effect)
    private final Set<Block> glassBlocks;
    private final Set<Block> capturedBlocks;  // Blocks above the point (used when captured)

    public CapturePointBlocks(CapturePoint point) {
        this.point = point;
        this.capturingBlocks = new HashSet<>();
        this.glassBlocks = new HashSet<>();
        this.capturedBlocks = new HashSet<>();
    }

    @Override
    public void setup() {
        capturingBlocks.clear();
        capturedBlocks.clear();
        glassBlocks.clear();

        // Scan for wool blocks (and BEACON as a special case) within the capture region.
        final Location max = point.getRegion().getMax();
        final Location min = point.getRegion().getMin();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = min.getWorld().getBlockAt(x, y, z);

                    if (block.getType().name().endsWith("_WOOL")) {
                        if (block.getRelative(BlockFace.UP).getType().name().endsWith("_STAINED_GLASS")) {
                            capturingBlocks.add(block);
                        } else {
                            capturedBlocks.add(block);
                        }
                    }

                    else if (block.getType().name().endsWith("_STAINED_GLASS")) {
                        glassBlocks.add(block);
                    }
                }
            }
        }
    }

    /**
     * Called every tick to update block colors according to the capture progress.
     *
     * Behavior:
     * <ul>
     *   <li>If the point is in CAPTURING state (a team is capturing a neutral point or contesting an owned point),
     *       blocks gradually change from white toward the capturing team's color.</li>
     *   <li>If the point is in REVERTING state (i.e. when nobody is contesting, the point is returning to its previous state),
     *       blocks gradually change from the previous color toward white.</li>
     *   <li>NEUTRAL and CAPTURED states do not update progressive effects.</li>
     * </ul>
     */
    public void tick() {
        // When the point is contested (owningTeam exists and capturingTeam is different), force CAPTURING state.
        CapturePoint.State state = point.getState();
        if (state != CapturePoint.State.CAPTURING && state != CapturePoint.State.REVERTING) {
            return;
        }

        int total = capturingBlocks.size();
        DyeColor targetColor;
        int desiredCount;
        double progress = point.getCaptureProgress();
        if (state == CapturePoint.State.CAPTURING) {
            // When capturing, blocks transition from white (or owning team's color) to the capturing team's color.
            targetColor = point.getOwningTeam() == null
                    ? Objects.requireNonNull(point.getCapturingTeam(), "Capturing team does not exist").getProperties().vanillaColor()
                    : DyeColor.WHITE;
            desiredCount = (int) Math.ceil(point.getOwningTeam() == null
                    ? progress * total
                    : (1.0 - progress) * total);
        } else { // REVERTING: point is returning to its previous state (owner or neutral)
            targetColor = point.getOwningTeam() == null ? DyeColor.WHITE : point.getOwningTeam().getProperties().vanillaColor();
            desiredCount = (int) Math.ceil(point.getOwningTeam() == null
                    ? (1.0 - progress) * total
                    : progress * total);
        }

        updateCapturingBlocks(targetColor, desiredCount);
    }

    /**
     * Updates the wool blocks so that the number of blocks showing the target color matches the desired count.
     *
     * @param targetColor  The DyeColor to apply.
     * @param desiredCount The desired number of blocks in the target color.
     */
    private void updateCapturingBlocks(DyeColor targetColor, int desiredCount) {
        Collection<Player> receivers = getReceivers();
        int currentCount = (int) capturingBlocks.stream()
                .filter(block -> block.getRelative(BlockFace.UP).getType().name().startsWith(targetColor.name()))
                .count();

        int diff = Math.abs(desiredCount - currentCount);
        if (diff > 0) {
            // Convert some blocks that are not the target color.
            List<Block> blocksToChange = capturingBlocks.stream()
                    .filter(block -> !block.getRelative(BlockFace.UP).getType().name().startsWith(targetColor.name()))
                    .limit(diff)
                    .toList();
            for (Block block : blocksToChange) {
                replaceBlock(block, targetColor, receivers, true);
            }
        }
    }

    /**
     * Returns nearby players to receive particle and sound effects.
     */
    private Collection<Player> getReceivers() {
        Location midpoint = UtilLocation.getMidpoint(point.getRegion().getMin(), point.getRegion().getMax());
        return midpoint.getNearbyPlayers(60);
    }

    /**
     * Replaces the block's color and sends particle and sound effects.
     *
     * @param block       The block to update.
     * @param color       The target DyeColor.
     * @param receivers   The players to receive the effect.
     * @param updateGlass Whether to update the glass above.
     */
    private void replaceBlock(Block block, DyeColor color, Collection<Player> receivers, boolean updateGlass) {
        // Only update wool blocks.
        if (block.getType().name().endsWith("_WOOL")) {
            Material wool = Material.valueOf(color.name() + "_WOOL");
            block.setType(wool);
            Particle.BLOCK.builder()
                    .extra(0)
                    .data(wool.createBlockData())
                    .count(20)
                    .offset(0.5, 0.1, 0.5)
                    .location(block.getLocation().toCenterLocation().add(0, 0.6, 0))
                    .receivers(receivers)
                    .spawn();
            new SoundEffect(Sound.BLOCK_WOOL_BREAK, 1.0f, 0.7f).play(block.getLocation());
        } else if (block.getType().name().endsWith("_STAINED_GLASS")) {
            Material glass = Material.valueOf(color.name() + "_STAINED_GLASS");
            block.setType(glass);
            updateGlass = false;
        }

        if (updateGlass) {
            Block above = block.getRelative(BlockFace.UP);
            if (above.getType().name().endsWith("_STAINED_GLASS")) {
                Material glass = Material.valueOf(color.name() + "_STAINED_GLASS");
                above.setType(glass);
            }
        }
    }

    /**
     * Sets all blocks in the captured area to white.
     */
    public void uncapture() {
        Collection<Player> receivers = getReceivers();
        for (Block block : capturedBlocks) {
            replaceBlock(block, DyeColor.WHITE, receivers, false);
        }

        for (Block block : glassBlocks) {
            replaceBlock(block, DyeColor.WHITE, receivers, false);
        }
    }

    /**
     * Sets all blocks in the capturing and captured areas to the specified team's color.
     *
     * @param team The team that now owns the point.
     */
    public void capture(Team team) {
        DyeColor color = team.getProperties().vanillaColor();
        Collection<Player> receivers = getReceivers();
        for (Block block : capturingBlocks) {
            replaceBlock(block, color, receivers, true);
        }
        for (Block block : capturedBlocks) {
            replaceBlock(block, color, receivers, false);
        }
        for (Block block : glassBlocks) {
            replaceBlock(block, color, receivers, false);
        }
    }

    @Override
    public void tearDown() {
        DyeColor color = DyeColor.WHITE;
        Collection<Player> receivers = getReceivers();
        for (Block block : capturingBlocks) {
            replaceBlock(block, color, receivers, true);
        }
        for (Block block : capturedBlocks) {
            replaceBlock(block, color, receivers, false);
        }
        for (Block block : glassBlocks) {
            replaceBlock(block, color, receivers, false);
        }

        capturingBlocks.clear();
        capturedBlocks.clear();
        glassBlocks.clear();
    }
}
