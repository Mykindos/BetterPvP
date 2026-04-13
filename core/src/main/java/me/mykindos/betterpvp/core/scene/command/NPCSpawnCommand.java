package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.npc.NPCFactory;
import me.mykindos.betterpvp.core.scene.npc.NPCFactoryManager;
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

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "NPC", "<red>Usage: <yellow>/npc spawn <factory> <type>");
            return;
        }

        final String factoryName = args[0];
        final String type = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final Optional<NPCFactory> factoryOpt = npcFactoryManager.getObject(factoryName.toLowerCase());
        if (factoryOpt.isEmpty()) {
            final String available = String.join(", ", npcFactoryManager.getObjects().keySet());
            UtilMessage.message(player, "NPC", "<red>Unknown factory: <yellow>" + factoryName);
            UtilMessage.message(player, "NPC", "<red>Available: <yellow>" + available);
            return;
        }

        final NPCFactory factory = factoryOpt.get();
        if (!Arrays.asList(factory.getTypes()).contains(type)) {
            final String available = String.join(", ", factory.getTypes());
            UtilMessage.message(player, "NPC", "<red>Unknown type: <yellow>" + type);
            UtilMessage.message(player, "NPC", "<red>Available for " + factoryName + ": <yellow>" + available);
            return;
        }

        final SceneObject spawned = factory.spawnDefault(player.getLocation(), type);
        UtilMessage.message(player, "NPC", "<green>Spawned <yellow>" + type + " <green>(ID <yellow>" + spawned.getId() + "<green>)");
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return npcFactoryManager.getObjects().keySet().stream()
                    .filter(k -> k.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            final NPCFactory factory = npcFactoryManager.getObject(args[0].toLowerCase()).orElse(null);
            if (factory != null) {
                return Arrays.stream(factory.getTypes())
                        .filter(t -> t.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }
        return super.processTabComplete(sender, args);
    }
}
