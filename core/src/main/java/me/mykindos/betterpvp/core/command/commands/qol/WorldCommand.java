package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.chat.ChatCallbacks;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.menu.GuiWorldManager;
import org.bukkit.entity.Player;

@Singleton
public class WorldCommand extends Command {

    private final WorldHandler worldHandler;
    private final ChatCallbacks callbacks;

    @Inject
    private WorldCommand(WorldHandler worldHandler, ChatCallbacks callbacks) {
        this.worldHandler = worldHandler;
        this.callbacks = callbacks;
    }

    @Override
    public String getName() {
        return "world";
    }

    @Override
    public String getDescription() {
        return "Use and administrate the server's worlds.";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new GuiWorldManager(worldHandler, callbacks, null).show(player);
    }
}
