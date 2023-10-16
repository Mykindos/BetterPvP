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
public class DisplaySelectSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "select";
    }

    @Override
    public String getDescription() {
        return "Select a display entity";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        displayEditorManager.startSelecting(player);
        UtilMessage.simpleMessage(player, "Display", "Punch a display entity in the next <alt2>15</alt2> seconds to <alt>select</alt> it .");
    }

}
