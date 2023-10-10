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
public class DisplayDeselectSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "deselect";
    }

    @Override
    public String getDescription() {
        return "Deselect a display entity";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final boolean deselected = displayEditorManager.selectDisplay(player, null);
        if (!deselected) {
            UtilMessage.simpleMessage(player, "Display", "You are not selecting a display.");
            return;
        }
        UtilMessage.simpleMessage(player, "Display", "Deselected display entity.");
    }

}
