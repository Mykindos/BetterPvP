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
        return "core.command.n-p-c-remove.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.remove.usage");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.remove.id.invalid", net.kyori.adventure.text.Component.text(args[0]));
            return;
        }

        final NPC npc = registry.getObject(id, NPC.class);
        if (npc == null) {
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.remove.not_found", net.kyori.adventure.text.Component.text(id));
            return;
        }

        npc.remove();
        UtilMessage.message(player, "core.prefix.command", "core.command.npc.remove.success", net.kyori.adventure.text.Component.text(id));
    }
}
