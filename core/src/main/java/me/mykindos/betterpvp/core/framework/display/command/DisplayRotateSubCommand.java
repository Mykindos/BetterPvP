package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.display.DisplayEditorManager;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SubCommand(DisplayTransformSubCommand.class)
public class DisplayRotateSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "rotate";
    }

    @Override
    public String getDescription() {
        return "core.command.display-rotate.description";
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return List.of("left", "right");
        } else if (args.length == 3) {
            return List.of("set", "add");
        } else if (args.length > 4 && args.length < 9) {
            return List.of("0");
        }
        return Collections.emptyList();
    }

    @Override
    public String getArgumentType(int arg) {

        return ArgumentType.NONE.name();
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        args = Arrays.copyOfRange(args, 1, args.length);
        if (args.length != 7 && args.length != 6) {
            UtilMessage.message(player, "core.prefix.display", "core.display.rotate.usage");
            return;
        }

        final Display selectedDisplay = displayEditorManager.getSelectedDisplay(player);
        if (selectedDisplay == null) {
            UtilMessage.message(player, "core.prefix.display", "core.display.not_selecting");
            return;
        }

        final String direction = args[0];
        boolean right = direction.equalsIgnoreCase("right") || direction.equalsIgnoreCase("r");
        if (!right && !direction.equalsIgnoreCase("left") && !direction.equalsIgnoreCase("l")) {
            UtilMessage.message(player, "core.prefix.display", "core.display.rotate.invalid_direction",
                    Component.text(direction));
            return;
        }

        final String type = args[1];
        if (!type.equalsIgnoreCase("add") && !type.equalsIgnoreCase("set")) {
            UtilMessage.message(player, "core.prefix.display", "core.display.rotate.usage_type");
            return;
        }

        float x;
        float y;
        float z;
        float w;
        try {
            w = Float.parseFloat(args[2]);
            x = Float.parseFloat(args[3]);
            y = Float.parseFloat(args[4]);
            z = Float.parseFloat(args[5]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "core.prefix.display", "core.display.rotate.invalid_values");
            return;
        }

        final Transformation permutated = selectedDisplay.getTransformation();
        final Quaternionf rotation = right ? permutated.getRightRotation() : permutated.getLeftRotation();
        boolean pivot = args.length == 7 && args[6].equalsIgnoreCase("pivot");
        if (args.length == 7 && !pivot) {
            UtilMessage.message(player, "core.prefix.display", "core.display.rotate.invalid_argument",
                    Component.text(args[7]));
            return;
        } else if (pivot) {
            x += permutated.getTranslation().x + permutated.getScale().x / 2;
            y += permutated.getTranslation().y;
            z += permutated.getTranslation().z + permutated.getScale().z / 2;
        }

        if (type.equalsIgnoreCase("set")) {
            rotation.setAngleAxis(Math.toRadians(w), x, y, z);
            UtilMath.rotateAround(permutated, permutated.getLeftRotation(), permutated.getRightRotation(), new Vector3f(x, y, z));
        } else {
            rotation.rotateAxis((float) Math.toRadians(w), x, y, z);
        }

        selectedDisplay.setTransformation(permutated);
        UtilMessage.message(player, "core.prefix.display", "core.display.rotate.success");
    }
}
