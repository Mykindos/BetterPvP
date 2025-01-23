package me.mykindos.betterpvp.core.npc.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Objects;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCListCommand extends Command {

    private final NPCRegistry registry;

    @Inject
    public NPCListCommand(NPCRegistry registry) {
        this.registry = registry;
    }

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
        for (NPC npc : registry.getObjects().values()) {
            final Component type = Component.text("Type: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(npc.getFactory().getName()).color(NamedTextColor.DARK_GREEN));
            final Component id = Component.text("ID: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(npc.getId()).color(NamedTextColor.GOLD));
            final Component name = Objects.requireNonNullElse(npc.getEntity().customName(), Component.text(npc.getEntity().getName()))
                    .applyFallbackStyle(NamedTextColor.GREEN);
            UtilMessage.message(player, "NPC", type.appendSpace().append(id).appendSpace().append(name));
            found = true;
        }

        if (!found) {
            UtilMessage.message(player, "NPC", "<red>No NPCs found");
        }
    }
}
