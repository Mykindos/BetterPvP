package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCRemoveCommand extends Command {

    private final SceneObjectRegistry registry;

    @Inject
    private NPCRemoveCommand(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Removes an NPC";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "NPC", "<red>Usage: <yellow>/npc remove <ID>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "NPC", "<red>Invalid ID: <yellow>" + args[0]);
            return;
        }

        final NPC npc = registry.getObject(id, NPC.class);
        if (npc == null) {
            UtilMessage.message(player, "NPC", "<red>No NPC found with ID: <yellow>" + id);
            return;
        }

        npc.remove();
        UtilMessage.message(player, "NPC", "<green>Removed NPC with ID: <yellow>" + id);
    }
}
