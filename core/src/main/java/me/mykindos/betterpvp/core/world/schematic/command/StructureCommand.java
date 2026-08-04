package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

/**
 * Builder tooling for structures — a build and its Mapper data-points saved and placed as one thing.
 *
 * @see me.mykindos.betterpvp.core.world.schematic.StructureFormat
 */
@Singleton
public class StructureCommand extends Command {

    static final String PREFIX = "Structure";

    @Inject
    public StructureCommand() {
        aliases.add("struct");
    }

    @Override
    public String getName() {
        return "structure";
    }

    @Override
    public String getDescription() {
        return "Save and paste builds together with their data-points";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.simpleMessage(player, PREFIX, "Usage: /structure <save|paste> <name>");
    }
}
