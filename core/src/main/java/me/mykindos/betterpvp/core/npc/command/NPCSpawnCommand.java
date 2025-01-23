package me.mykindos.betterpvp.core.npc.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.NPCFactoryManager;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Optional;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCSpawnCommand extends Command {

    private final NPCFactoryManager npcFactoryManager;

    @Inject
    private NPCSpawnCommand(NPCFactoryManager npcFactoryManager) {
        this.npcFactoryManager = npcFactoryManager;
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Spawns an NPC";
    }

    // /npc spawn <type> <name>
    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "NPC", "<red>Usage: <yellow>/npc spawn <type> <name>");
            return;
        }

        String type = args[0];
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final Optional<NPCFactory> factoryOpt = npcFactoryManager.getObject(type.toLowerCase());
        if (factoryOpt.isEmpty()) {
            final String types = String.join(", ", npcFactoryManager.getObjects().keySet());
            UtilMessage.message(player, "NPC", "<red>Invalid NPC type: <yellow>" + type);
            UtilMessage.message(player, "NPC", "<red>Available types: <yellow>" + types);
            return;
        }

        final NPCFactory factory = factoryOpt.get();
        try {
            final NPC npc = factory.spawnDefault(player.getLocation(), name);
            UtilMessage.message(player, "NPC", "<green>Spawned NPC <yellow>" + name + " <green>with ID <yellow>" + npc.getId());
        } catch (UnsupportedOperationException e) {
            UtilMessage.message(player, "NPC", "<red>This NPCFactory does not support spawning NPCs!");
        }
    }
}
