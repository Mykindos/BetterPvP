package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Objects;

@Singleton
@SubCommand(NPCCommand.class)
public class NPCListCommand extends Command {

    private final SceneObjectRegistry registry;

    @Inject
    public NPCListCommand(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "core.command.n-p-c-list.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) {
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.list.usage");
            return;
        }

        UtilMessage.message(player, "core.prefix.command", "core.command.npc.list.header");
        boolean found = false;
        for (NPC npc : registry.getObjects(NPC.class)) {
            final Component type = Translations.component("core.command.npc.list.entry.type_label")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(npc.getFactory().getName()).color(NamedTextColor.DARK_GREEN));
            final Component id = Translations.component("core.command.npc.list.entry.id_label")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(npc.getId()).color(NamedTextColor.GOLD));
            final Component name = Objects.requireNonNullElse(npc.getEntity().customName(), Component.text(npc.getEntity().getName()))
                    .applyFallbackStyle(NamedTextColor.GREEN);
            UtilMessage.message(player, "core.prefix.command", type.appendSpace().append(id).appendSpace().append(name));
            found = true;
        }

        if (!found) {
            UtilMessage.message(player, "core.prefix.command", "core.command.npc.list.none");
        }
    }
}
