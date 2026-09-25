package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreview;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreviews;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * {@code /structure ghost <name>} shows a ghost of a structure on the block you are looking at, turned to your facing:
 * green if every block it needs is free, red if anything is in the way. {@code /structure ghost} on its own takes it
 * away. Only you can see it.
 */
@SubCommand(StructureCommand.class)
public class StructureGhostSubCommand extends Command {

    private static final int REACH = 64;

    private final SchematicService schematics;
    private final GhostPreviews previews;

    @Inject
    public StructureGhostSubCommand(SchematicService schematics, GhostPreviews previews) {
        this.schematics = schematics;
        this.previews = previews;
    }

    @Override
    public String getName() {
        return "ghost";
    }

    @Override
    public String getDescription() {
        return "Show a ghost of a structure where you look, only to you";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            previews.close(player);
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "Ghost cleared.");
            return;
        }

        final Optional<Schematic> loaded = schematics.load(args[0]);
        if (loaded.isEmpty()) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>No structure called '%s'.", args[0]);
            return;
        }

        final Block target = player.getTargetBlockExact(REACH);
        if (target == null) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>Look at a block to place the ghost on.");
            return;
        }

        final Location anchor = target.getRelative(BlockFace.UP).getLocation();
        anchor.setYaw(player.getLocation().getYaw());
        final SchematicPlacement placement = SchematicPlacement.facing(loaded.get(), anchor);
        final boolean fits = placement.getBlocks().stream()
                .allMatch(block -> player.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).isReplaceable());

        final GhostPreview preview = previews.open(player, loaded.get());
        preview.setValid(fits);
        preview.show(anchor, placement.getQuarterTurns());

        UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                "Showing <green>%s</green> as %d display(s) for %d block(s). It %s.", args[0],
                preview.displayCount(placement.getQuarterTurns()), placement.getBlocks().size(),
                fits ? "<green>fits</green>" : "<red>is blocked</red>");
    }
}
