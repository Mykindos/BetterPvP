package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.schematic.StructureAnchor;
import me.mykindos.betterpvp.core.world.schematic.StructureCapture;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Optional;

/**
 * {@code /structure save <name>} — captures the WorldEdit selection and every data-point inside it.
 * <p>
 * Where you stand and which way you face becomes the structure's anchor, so stand on the block that should land on the
 * marker and face the way the build should point.
 */
@SubCommand(StructureCommand.class)
public class StructureSaveSubCommand extends Command {

    private final StructureCapture capture;
    private final SchematicService schematics;

    @Inject
    public StructureSaveSubCommand(StructureCapture capture, SchematicService schematics) {
        this.capture = capture;
        this.schematics = schematics;
    }

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "Capture your selection and its data-points into one structure";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "Usage: /structure save <name>");
            return;
        }

        final Optional<Schematic> captured = capture.capture(player);
        if (captured.isEmpty()) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                    "<red>Make a WorldEdit selection first.");
            return;
        }

        final Schematic schematic = captured.get();
        final Optional<File> saved = schematics.save(args[0], schematic);
        if (saved.isEmpty()) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>Could not write that structure.");
            return;
        }

        UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                "Saved <green>%s</green> — %d blocks, %d data-point(s), facing %.0f°.",
                saved.get().getName(), schematic.blockCount(), schematic.getRegions().size(),
                schematic.getAnchorYaw());

        // Which spot the structure will land on is the one thing a builder cannot see afterwards, and getting it wrong
        // is invisible until a paste is several blocks out.
        if (StructureAnchor.isAnchored(schematic)) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                    "<gray>Anchored to its <white>%s</white> marker.", StructureAnchor.POINT);
        } else {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                    "<yellow>No <white>%s</white> marker — anchored where you were standing.",
                    StructureAnchor.POINT);
        }
    }
}
