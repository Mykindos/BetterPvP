package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.display.DisplayEditorManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SubCommand(DisplayTransformSubCommand.class)
public class DisplayTranslateSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "translate";
    }

    @Override
    public String getDescription() {
        return "Translate a display entity";
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return List.of("set", "add");
        } else if (args.length > 4 && args.length < 9) {
            return List.of("0");
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        args = Arrays.copyOfRange(args, 1, args.length);
        if (args.length != 4) {
            UtilMessage.simpleMessage(player, "Display", "Usage: /display transform translate <set|add> <x> <y> <z>");
            return;
        }

        final Display selectedDisplay = displayEditorManager.getSelectedDisplay(player);
        if (selectedDisplay == null) {
            UtilMessage.simpleMessage(player, "Display", "You are not selecting a display.");
            return;
        }

        final String type = args[0];
        if (!type.equalsIgnoreCase("add") && !type.equalsIgnoreCase("set")) {
            UtilMessage.simpleMessage(player, "Display", "Usage: /display transform translate <set|add> <x> <y> <z>");
            return;
        }

        final float x, y, z;
        try {
            x = Float.parseFloat(args[1]);
            y = Float.parseFloat(args[2]);
            z = Float.parseFloat(args[3]);
        } catch (NumberFormatException e) {
            UtilMessage.simpleMessage(player, "Display", "Invalid translation values. Must be numbers.");
            return;
        }

        final Transformation permutated = selectedDisplay.getTransformation();
        final Vector3f translation = permutated.getTranslation();
        if (type.equalsIgnoreCase("set")) {
            translation.set(x, y, z);
        } else {
            translation.add(x, y, z);
        }

        selectedDisplay.setTransformation(permutated);
        UtilMessage.simpleMessage(player, "Display", "Translated display entity.");
    }
}
