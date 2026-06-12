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
        return "core.command.display-remove.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Display selectedDisplay = displayEditorManager.getSelectedDisplay(player);
        final boolean deselected = displayEditorManager.selectDisplay(player, null);
        if (!deselected) {
            UtilMessage.message(player, "core.prefix.display", "core.display.not_selecting");
            return;
        }

        selectedDisplay.remove();
        UtilMessage.message(player, "core.prefix.display", "core.display.despawned");
    }
}
