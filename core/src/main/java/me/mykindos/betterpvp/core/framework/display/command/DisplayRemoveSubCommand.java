package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.display.DisplayEditorManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(DisplayCommand.class)
public class DisplayRemoveSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Remove a display entity";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Display selectedDisplay = displayEditorManager.getSelectedDisplay(player);
        final boolean deselected = displayEditorManager.selectDisplay(player, null);
        if (!deselected) {
            UtilMessage.simpleMessage(player, "Display", "You are not selecting a display.");
            return;
        }

        selectedDisplay.remove();
        UtilMessage.simpleMessage(player, "Display", "Despawned selected display.");
    }
}
