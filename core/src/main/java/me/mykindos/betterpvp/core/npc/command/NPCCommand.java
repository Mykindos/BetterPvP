package me.mykindos.betterpvp.core.npc.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class NPCCommand extends Command {

    @Override
    public String getName() {
        return "npc";
    }

    @Override
    public String getDescription() {
        return "NPC Commands";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "NPC", "NPC Help");
        UtilMessage.message(player, "NPC", "<alt2>/npc spawn <type> <name></alt2> - Spawns an NPC");
        UtilMessage.message(player, "NPC", "<alt2>/npc remove <ID></alt2> - Removes an NPC");
        UtilMessage.message(player, "NPC", "<alt2>/npc list</alt2> - Lists all NPCs");
    }
}
