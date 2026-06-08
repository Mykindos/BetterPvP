package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.display.DisplayEditorManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
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
        return "core.command.display-interpolation.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 2) {
            UtilMessage.message(player, "core.prefix.display", "core.display.interpolation.usage");
            return;
        }

        final Display selectedDisplay = displayEditorManager.getSelectedDisplay(player);
        if (selectedDisplay == null) {
            UtilMessage.message(player, "core.prefix.display", "core.display.not_selecting");
            return;
        }

        final String type = args[0].toLowerCase();
        final boolean delay = type.equalsIgnoreCase("delay");
        if (!delay && !type.equalsIgnoreCase("duration")) {
            UtilMessage.message(player, "core.prefix.display", "core.display.interpolation.usage_ticks");
            return;
        }

        final int ticks;
        try {
            ticks = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "core.prefix.display", "core.display.interpolation.invalid_ticks");
            return;
        }

        if (delay) {
            selectedDisplay.setInterpolationDelay(ticks);
        } else {
            selectedDisplay.setInterpolationDuration(ticks);
        }

        UtilMessage.message(player, "core.prefix.display", "core.display.interpolation.set",
                Component.text(type), Component.text(ticks));
    }
}
