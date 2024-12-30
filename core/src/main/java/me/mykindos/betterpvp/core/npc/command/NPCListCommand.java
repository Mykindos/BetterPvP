package me.mykindos.betterpvp.core.npc.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCListCommand extends Command {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists all active NPCs";
    }

    // /npc list
    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) {
            UtilMessage.message(player, "NPC", "<red>Usage: <yellow>/npc list");
            return;
        }

        UtilMessage.message(player, "NPC", "Active NPCs:");
        boolean found = false;
        for (NPCRegistry registry : CitizensAPI.getNPCRegistries()) {
            for (NPC npc : registry) {
                UtilMessage.message(player, "NPC", "<yellow>Type: <dark_green>" + registry.getName() + " <yellow>ID: <gold>" + npc.getId() + " <yellow>Name: <green>" + npc.getName());
                found = true;
            }
        }

        if (!found) {
            UtilMessage.message(player, "NPC", "<red>No NPCs found");
        }
    }
}
