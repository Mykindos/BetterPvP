package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicAnimator;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.schematic.StructureTransform;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * {@code /structure paste <name>} — places a structure where you stand, turned to match the way you face.
 * <p>
 * A builder's check on what a berth will do at runtime: the rotation is worked out exactly as a real paste does, from
 * the difference between the structure's captured facing and the target's. The data-points are reported rather than
 * registered — they belong to whoever pasted the structure, not to the world's Mapper file.
 */
@SubCommand(StructureCommand.class)
public class StructurePasteSubCommand extends Command {

    private final SchematicService schematics;
    private final SchematicAnimator animator;

    @Inject
    public StructurePasteSubCommand(SchematicService schematics, SchematicAnimator animator) {
        this.schematics = schematics;
        this.animator = animator;
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

        final Schematic schematic = loaded.get();
        final int turns = StructureTransform.quarterTurnsBetween(schematic.getAnchorYaw(), player.getLocation().getYaw());

        animator.paste(schematic, player.getLocation(), turns);
        final List<Region> regions = animator.pasteRegions(schematic, player.getLocation(), turns);

        UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                "Pasted <green>%s</green> — %d blocks, %d data-point(s), turned %d×90°.",
                args[0], schematic.blockCount(), regions.size(), turns);
    }
}
