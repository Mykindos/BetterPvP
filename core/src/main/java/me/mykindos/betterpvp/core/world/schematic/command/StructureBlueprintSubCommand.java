package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.blueprint.BlueprintSessions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        final Component prefix = Translations.component("core.prefix.construction");
        if (args.length < 1) {
            UtilMessage.message(player, prefix, Translations.component("core.construction.command.blueprint.usage"));
            return;
        }

        final Optional<StructureType> type = catalogue.find(args[0]);
        if (type.isEmpty()) {
            UtilMessage.message(player, prefix, Translations.component("core.construction.command.blueprint.unknown",
                    Component.text(args[0])).color(NamedTextColor.RED));
            return;
        }
        player.getInventory().addItem(blueprints.blueprintFor(type.get()));
        UtilMessage.message(player, prefix, Translations.component("core.construction.command.blueprint.given",
                type.get().getDisplayName().color(NamedTextColor.GREEN)));
    }
}
