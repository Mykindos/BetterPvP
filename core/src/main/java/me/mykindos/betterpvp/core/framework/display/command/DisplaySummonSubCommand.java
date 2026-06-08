package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.display.DisplayEditorManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
@SubCommand(DisplayCommand.class)
public class DisplaySummonSubCommand extends Command {

    @Inject
    private DisplayEditorManager displayEditorManager;

    @Override
    public String getName() {
        return "summon";
    }

    @Override
    public String getDescription() {
        return "core.command.display-summon.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "core.prefix.display", "core.display.summon.usage");
            return;
        }

        Display display;
        final Location location = player.getLocation();
        location.setYaw(0);
        location.setPitch(0);
        switch (args[0].toLowerCase()) {
            case "block" -> {
                if (args.length < 2) {
                    UtilMessage.message(player, "core.prefix.display", "core.display.summon.block_usage");
                    return;
                }
                Material material = Material.matchMaterial(args[1]);
                if (material == null) {
                    UtilMessage.message(player, "core.prefix.display", "core.display.summon.invalid_block",
                            Component.text(args[1]));
                    return;
                }
                display = player.getWorld().spawn(location, BlockDisplay.class);
                ((BlockDisplay) display).setBlock(Bukkit.createBlockData(material));
            }
            default -> {
                UtilMessage.message(player, "core.prefix.display", "core.display.summon.invalid_type",
                        Component.text(args[0]));
                return;
            }
        }

        // tag
        displayEditorManager.selectDisplay(player, display);
        UtilMessage.message(player, "core.prefix.display", "core.display.summon.success");
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("block");
        } else if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "block":
                    return List.of(Arrays.stream(Material.values()).map(Material::translationKey).toArray(String[]::new));
                case "item":
                case "billboard":
                default:
                    return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
