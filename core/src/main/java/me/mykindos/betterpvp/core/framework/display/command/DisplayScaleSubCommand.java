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
public class DisplayScaleSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "scale";
    }

    @Override
    public String getDescription() {
        return "core.command.display-scale.description";
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
            UtilMessage.message(player, "core.prefix.display", "core.display.scale.usage");
            return;
        }

        final Display selectedDisplay = displayEditorManager.getSelectedDisplay(player);
        if (selectedDisplay == null) {
            UtilMessage.message(player, "core.prefix.display", "core.display.not_selecting");
            return;
        }

        final String type = args[0];
        if (!type.equalsIgnoreCase("add") && !type.equalsIgnoreCase("set")) {
            UtilMessage.message(player, "core.prefix.display", "core.display.scale.usage");
            return;
        }

        final float x, y, z;
        try {
            x = Float.parseFloat(args[1]);
            y = Float.parseFloat(args[2]);
            z = Float.parseFloat(args[3]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "core.prefix.display", "core.display.scale.invalid_values");
            return;
        }

        final Transformation permutated = selectedDisplay.getTransformation();
        final Vector3f scale = permutated.getScale();
        if (type.equalsIgnoreCase("set")) {
            scale.set(x, y, z);
        } else {
            scale.add(x, y, z);
        }

        selectedDisplay.setTransformation(permutated);
        UtilMessage.message(player, "core.prefix.display", "core.display.scale.success");
    }
}
