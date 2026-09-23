package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.blueprint.BlueprintSessions;
import org.bukkit.entity.Player;

import java.util.Optional;

/** {@code /structure blueprint <type>} gives you a blueprint for a structure type, to try building it. */
@SubCommand(StructureCommand.class)
public class StructureBlueprintSubCommand extends Command {

    private final StructureCatalogue catalogue;
    private final BlueprintSessions blueprints;

    @Inject
    public StructureBlueprintSubCommand(StructureCatalogue catalogue, BlueprintSessions blueprints) {
        this.catalogue = catalogue;
        this.blueprints = blueprints;
    }

    @Override
    public String getName() {
        return "blueprint";
    }

    @Override
    public String getDescription() {
        return "Get a blueprint for a structure type";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "Usage: /structure blueprint <type>");
            return;
        }

        final Optional<StructureType> type = catalogue.find(args[0]);
        if (type.isEmpty()) {
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>No structure type called '%s'.", args[0]);
            return;
        }
        player.getInventory().addItem(blueprints.blueprintFor(type.get()));
        UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "Here is a blueprint for <green>%s</green>.", args[0]);
    }
}
