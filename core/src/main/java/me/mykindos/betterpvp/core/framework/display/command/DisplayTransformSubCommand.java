package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.display.DisplayEditorManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(DisplayCommand.class)
public class DisplayTransformSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "transform";
    }

    @Override
    public String getDescription() {
        return "core.command.display-transform.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "core.prefix.display", "core.display.transform.usage");
    }
}
