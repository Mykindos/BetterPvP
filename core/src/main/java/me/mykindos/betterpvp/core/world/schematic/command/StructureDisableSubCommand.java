package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.view.StructureViews;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;

/** {@code /structure disable} disables the structure you are looking at until it is repaired. */
@SubCommand(StructureCommand.class)
public class StructureDisableSubCommand extends Command {

    private final StructureViews views;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;

    @Inject
    public StructureDisableSubCommand(StructureViews views, ConstructionService construction,
                                      StructureCatalogue catalogue) {
        this.views = views;
        this.construction = construction;
        this.catalogue = catalogue;
    }

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public String getDescription() {
        return "Disable the structure you are looking at";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Component prefix = Translations.component("core.prefix.construction");
        final Block target = player.getTargetBlockExact(24);
        final Optional<PlacedStructure> structure = target == null ? Optional.empty() : views.structureAt(target);
        if (structure.isEmpty()) {
            UtilMessage.message(player, prefix,
                    ConstructionResult.reason("core.construction.command.disable.no_structure"));
            return;
        }

        final ConstructionResult result = construction.disable(target.getWorld(), structure.get().getId());
        if (!result.isSuccess()) {
            if (result.getReason() != null) {
                UtilMessage.message(player, prefix, result.getReason());
            }
            return;
        }
        final Component name = catalogue.find(structure.get().getType())
                .map(StructureType::getDisplayName)
                .orElseGet(() -> Component.text(structure.get().getType()));
        UtilMessage.message(player, prefix, Translations.component("core.construction.command.disable.done",
                name.color(NamedTextColor.WHITE)).color(NamedTextColor.GREEN));
    }
}
