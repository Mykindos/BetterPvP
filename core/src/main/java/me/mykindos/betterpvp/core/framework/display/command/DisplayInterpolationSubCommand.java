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
public class DisplayInterpolationSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "interpolation";
    }

    @Override
    public String getDescription() {
        return "Change the interpolation of a display entity";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 2) {
            UtilMessage.simpleMessage(player, "Display", "Usage: /display interpolation <delay|duration> <value>");
            return;
        }

        final Display selectedDisplay = displayEditorManager.getSelectedDisplay(player);
        if (selectedDisplay == null) {
            UtilMessage.simpleMessage(player, "Display", "You are not selecting a display.");
            return;
        }

        final String type = args[0].toLowerCase();
        final boolean delay = type.equalsIgnoreCase("delay");
        if (!delay && !type.equalsIgnoreCase("duration")) {
            UtilMessage.simpleMessage(player, "Display", "Usage: /display interpolation <delay|duration> <ticks>");
            return;
        }

        final int ticks;
        try {
            ticks = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            UtilMessage.simpleMessage(player, "Display", "Invalid ticks, must be an integer.");
            return;
        }

        if (delay) {
            selectedDisplay.setInterpolationDelay(ticks);
        } else {
            selectedDisplay.setInterpolationDuration(ticks);
        }

        UtilMessage.simpleMessage(player, "Display", "Set interpolation " + type + " to " + ticks + " ticks.");
    }
}
