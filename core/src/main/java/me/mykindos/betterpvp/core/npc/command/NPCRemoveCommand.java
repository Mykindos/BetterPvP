package me.mykindos.betterpvp.core.npc.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.NPCFactoryManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCRemoveCommand extends Command {

    private final NPCFactoryManager npcFactoryManager;

    @Inject
    private NPCRemoveCommand(NPCFactoryManager npcFactoryManager) {
        this.npcFactoryManager = npcFactoryManager;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Removes an NPC";
    }

    // /npc remove <type> <ID>
    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "NPC", "<red>Usage: <yellow>/npc remove <type> <ID>");
            return;
        }

        String type = args[0];
        final Optional<NPCFactory> factoryOpt = npcFactoryManager.getObject(type.toLowerCase());
        if (factoryOpt.isEmpty()) {
            final String types = String.join(", ", npcFactoryManager.getObjects().keySet());
            UtilMessage.message(player, "NPC", "<red>Invalid NPC type: <yellow>" + type);
            UtilMessage.message(player, "NPC", "<red>Available types: <yellow>" + types);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "NPC", "<red>Invalid ID: <yellow>" + args[1]);
            return;
        }

        final NPCFactory factory = factoryOpt.get();
        try {
            factory.getRegistry().getById(id).destroy();
            UtilMessage.message(player, "NPC", "<green>Removed NPC with ID: <yellow>" + id);
        } catch (IllegalArgumentException e) {
            UtilMessage.message(player, "NPC", "<red>No NPC found with ID: <yellow>" + id);
        }
    }
}
