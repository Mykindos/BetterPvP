package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.schematic.LayerPlan;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.schematic.SchematicRenderer;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /structure layers <name> <count>} pastes only the first {@code count} layers of a structure where you stand,
 * the way it would look part way through being built, so a builder can check how their {@code layer:<n>} markers order
 * it. Taken back out with {@code /structure undo}.
 */
@SubCommand(StructureCommand.class)
public class StructureLayersSubCommand extends Command {

    private final SchematicService schematics;
    private final SchematicRenderer renderer;
    private final StructurePasteHistory history;

    @Inject
    public StructureLayersSubCommand(SchematicService schematics, SchematicRenderer renderer,
                                     StructurePasteHistory history) {
        this.schematics = schematics;
        this.renderer = renderer;
        this.history = history;
    }

    @Override
    public String getName() {
        return "layers";
    }

    @Override
    public String getDescription() {
        return "Paste the first few layers of a structure, as it looks part way built";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "Usage: /structure layers <name> <count>");
            return;
        }

        final Optional<Schematic> loaded = schematics.load(args[0]);
        if (loaded.isEmpty()) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>No structure called '%s'.", args[0]);
            return;
        }

        final int count;
        try {
            count = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>'%s' is not a number.", args[1]);
            return;
        }

        final LayerPlan plan = LayerPlan.of(loaded.get());
        final SchematicPlacement placement = SchematicPlacement.facing(loaded.get(), player.getLocation());
        final int shown = Math.clamp(count, 0, plan.size());
        final List<Schematic.PlacedBlock> undo = new ArrayList<>();
        for (int layer = 0; layer < shown; layer++) {
            undo.addAll(renderer.paste(placement, plan.layer(layer)));
        }
        history.record(player.getUniqueId(), player.getWorld(), undo);

        UtilMessage.simpleMessage(player, StructureCommand.PREFIX,
                "Pasted <green>%d</green> of <green>%d</green> layers of <green>%s</green>.", shown, plan.size(), args[0]);
    }
}
