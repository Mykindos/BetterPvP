package me.mykindos.betterpvp.core.npc.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCRemoveCommand extends Command {

    private final NPCRegistry npcRegistry;

    @Inject
    private NPCRemoveCommand(NPCRegistry npcRegistry) {
        this.npcRegistry = npcRegistry;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Removes an NPC";
    }

    // /npc remove <ID>
    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "NPC", "<red>Usage: <yellow>/npc remove <ID>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "NPC", "<red>Invalid ID: <yellow>" + args[1]);
            return;
        }

        try {
            npcRegistry.getNPC(id).remove();
            UtilMessage.message(player, "NPC", "<green>Removed NPC with ID: <yellow>" + id);
        } catch (IllegalArgumentException e) {
            UtilMessage.message(player, "NPC", "<red>No NPC found with ID: <yellow>" + id);
        }
    }
}
