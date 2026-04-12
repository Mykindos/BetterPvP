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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
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
            UtilMessage.message(player, "NPC", "<red>Usage: <yellow>/npc spawn <factory> <type>");
            return;
        }

        String factoryName = args[0];
        String type = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final Optional<NPCFactory> factoryOpt = npcFactoryManager.getObject(factoryName.toLowerCase());
        if (factoryOpt.isEmpty()) {
            final String types = String.join(", ", npcFactoryManager.getObjects().keySet());
            UtilMessage.message(player, "NPC", "<red>Invalid NPC factory: <yellow>" + factoryName);
            UtilMessage.message(player, "NPC", "<red>Available factories: <yellow>" + types);
            return;
        }

        final NPCFactory factory = factoryOpt.get();
        // check if valid type
        if (!Arrays.asList(factory.getTypes()).contains(type)) {
            final String types = String.join(", ", factory.getTypes());
            UtilMessage.message(player, "NPC", "<red>Invalid NPC type: <yellow>" + type);
            UtilMessage.message(player, "NPC", "<red>Available types for factory " + factoryName + ": <yellow>" + types);
            return;
        }

        try {
            final NPC npc = factory.spawnDefault(player.getLocation(), type);
            UtilMessage.message(player, "NPC", "<green>Spawned NPC <yellow>" + type + " <green>with ID <yellow>" + npc.getId());
        } catch (UnsupportedOperationException e) {
            UtilMessage.message(player, "NPC", "<red>This NPCFactory does not support spawning NPCs!");
        }
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return this.npcFactoryManager.getObjects().keySet()
                    .stream()
                    .filter(key -> key.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            final String factoryName = args[0];
            final NPCFactory factory = this.npcFactoryManager.getObject(factoryName.toLowerCase()).orElse(null);
            if (factory != null) {
                return Arrays.stream(factory.getTypes())
                        .filter(type -> type.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }
        return super.processTabComplete(sender, args);
    }
}
