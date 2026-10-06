package me.mykindos.betterpvp.core.resourcepack.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.resourcepack.ResourcePackService;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class ReloadPacksCommand extends Command {

    private final ResourcePackService service;

    @Inject
    public ReloadPacksCommand(ResourcePackService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "reloadpacks";
    }

    @Override
    public String getDescription() {
        return "core.command.reloadpacks.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        service.reload();
        UtilMessage.message(player, COMMAND_PREFIX, "core.command.reloadpacks.success", Component.text(service.getChannel()));
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

}
