package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.display.DisplayEditorManager;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(DisplayCommand.class)
public class DisplaySelectSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "select";
    }

    @Override
    public String getDescription() {
        return "core.command.display-select.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        displayEditorManager.startSelecting(player);
        UtilMessage.message(player, "core.prefix.display", "core.display.select.prompt",
                Component.text("15", NamedTextColor.YELLOW),
                Translations.component("core.display.select.action").color(NamedTextColor.GREEN));
    }

}
