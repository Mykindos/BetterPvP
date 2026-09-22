package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.schematic.SchematicRenderer;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * {@code /structure paste <name>} places a structure where you stand, turned to match the way you face.
 * <p>
 * A builder's check on what a berth will do at runtime: the rotation is worked out exactly as a real paste does, from
 * the difference between the structure's captured facing and the target's. The data-points are reported rather than
 * registered, since they belong to whoever pasted the structure, not to the world's Mapper file. {@code /structure undo}
 * takes it back out.
 */
@SubCommand(StructureCommand.class)
public class StructurePasteSubCommand extends Command {

    private final SchematicService schematics;
    private final SchematicRenderer renderer;
    private final StructurePasteHistory history;

    @Inject
    public StructurePasteSubCommand(SchematicService schematics, SchematicRenderer renderer,
                                    StructurePasteHistory history) {
        this.schematics = schematics;
        this.renderer = renderer;
        this.history = history;
    }

    @Override
    public String getName() {
        return "paste";
    }

    @Override
    public String getDescription() {
        return "Paste a structure where you stand, rotated to your facing";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "Usage: /structure paste <name>");
            return;
        }

        final Optional<Schematic> loaded = schematics.load(args[0]);
        if (loaded.isEmpty()) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                    "<red>No structure called '%s'.", args[0]);
            return;
        }

        final SchematicPlacement placement = SchematicPlacement.facing(loaded.get(), player.getLocation());
        history.record(player.getUniqueId(), player.getWorld(), renderer.paste(placement));

        UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                "Pasted <green>%s</green>: %d blocks, %d data-point(s), turned %d×90°.",
                args[0], placement.getBlocks().size(), placement.markers().size(), placement.getQuarterTurns());
    }
}
