package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.ClanArgument;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Singleton
public class BrigadierClansCommand extends BrigadierCommand {
    private final ClanManager clanManager;

    @Inject
    protected BrigadierClansCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clientManager);
        this.clanManager = clanManager;
        this.getAliases().addAll(List.of("brigadierc", "brigadierf", "brigadierfaction"));
    }

    @Override
    public String getName() {
        return "brigadierclans";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return Commands.literal(getName())
                .executes(context -> {
                    if (!(context.getSource().getExecutor() instanceof Player player)) return Command.SINGLE_SUCCESS;

                    Clan clan = clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgument.MUST_BE_IN_A_CLAN_EXCEPTION.create());
                    openClanMenu(player, clan, clan);
                    return Command.SINGLE_SUCCESS;
                });
    }

    @Override
    public String getDescription() {
        return "Shows the clan Menu";
    }

    private void openClanMenu(Player player, Clan playerClan, Clan clan) {
        // We swap back to main thread, because the menu is not thread-safe and can lead to players having a bugged inventory (dupe items)
        UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> new ClanMenu(player, playerClan, clan).show(player));
    }
}
