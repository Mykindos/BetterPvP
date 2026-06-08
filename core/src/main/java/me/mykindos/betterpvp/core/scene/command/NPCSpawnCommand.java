package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectFactoryManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCSpawnCommand extends Command {

    private final SceneObjectFactoryManager npcFactoryManager;

    @Inject
    private NPCSpawnCommand(SceneObjectFactoryManager npcFactoryManager) {
        this.npcFactoryManager = npcFactoryManager;
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "core.command.n-p-c-spawn.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.spawn.usage");
            return;
        }

        final String factoryName = args[0];
        final String type = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final Optional<SceneObjectFactory> factoryOpt = npcFactoryManager.getObject(factoryName.toLowerCase());
        if (factoryOpt.isEmpty()) {
            final String available = String.join(", ", npcFactoryManager.getObjects().keySet());
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.spawn.factory.unknown", net.kyori.adventure.text.Component.text(factoryName));
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.spawn.factory.available", net.kyori.adventure.text.Component.text(available));
            return;
        }

        final SceneObjectFactory factory = factoryOpt.get();
        if (!Arrays.asList(factory.getTypes()).contains(type)) {
            final String available = String.join(", ", factory.getTypes());
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.spawn.type.unknown", net.kyori.adventure.text.Component.text(type));
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.spawn.type.available_for", net.kyori.adventure.text.Component.text(factoryName), net.kyori.adventure.text.Component.text(available));
            return;
        }

        final SceneObject spawned = factory.spawnDefault(player.getLocation(), type);
        UtilMessage.message(player, "core.prefix.command", "core.command.npc.spawn.success", net.kyori.adventure.text.Component.text(type), net.kyori.adventure.text.Component.text(spawned.getId()));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return npcFactoryManager.getObjects().keySet().stream()
                    .filter(k -> k.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            final SceneObjectFactory factory = npcFactoryManager.getObject(args[0].toLowerCase()).orElse(null);
            if (factory != null) {
                return Arrays.stream(factory.getTypes())
                        .filter(t -> t.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }
        return super.processTabComplete(sender, args);
    }
}
