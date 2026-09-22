package me.mykindos.betterpvp.core.world.schematic.command;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.schematic.SchematicRenderer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** {@code /structure undo} takes your most recent structure paste back out. */
@SubCommand(StructureCommand.class)
public class StructureUndoSubCommand extends Command {

    private final SchematicRenderer renderer;
    private final StructurePasteHistory history;

    @Inject
    public StructureUndoSubCommand(SchematicRenderer renderer, StructurePasteHistory history) {
        this.renderer = renderer;
        this.history = history;
    }

    @Override
    public String getName() {
        return "undo";
    }

    @Override
    public String getDescription() {
        return "Take your most recent structure paste back out";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        history.takeLatest(player.getUniqueId()).ifPresentOrElse(paste -> {
            final World world = Bukkit.getWorld(paste.getWorld());
            if (world == null) {
                UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>That paste's world is no longer loaded.");
                return;
            }
            renderer.restore(world, paste.getUndo());
            UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "Undid a paste of %d blocks.", paste.getUndo().size());
        }, () -> UtilMessage.simpleMessage(player, StructureCommand.PREFIX, "<red>Nothing to undo."));
    }
}
