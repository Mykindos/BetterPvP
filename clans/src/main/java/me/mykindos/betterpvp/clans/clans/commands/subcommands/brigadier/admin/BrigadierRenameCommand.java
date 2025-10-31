package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;


@Singleton
@CustomLog
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierRenameCommand extends ClanBrigadierCommand {
    private final IFilterService filterService;
    @Inject
    protected BrigadierRenameCommand(ClientManager clientManager, ClanManager clanManager, IFilterService filterService) {
        super(clientManager, clanManager);
        this.filterService = filterService;
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "rename";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Renames your current clan";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                .then(Commands.argument("Clan Name", BPvPClansArgumentTypes.clanName())
                        .executes(context -> {
                            final String name = context.getArgument("Clan Name", String.class);
                            final Player executor = getPlayerFromExecutor(context);
                            final Clan executorClan = getClanByExecutor(context);
                            filterService.isFiltered(name).thenAccept((filtered) -> {
                                if (filtered) {
                                    context.getSource().getSender()
                                            .sendMessage(UtilMessage.deserialize("<red>" +
                                                    ClanArgumentException.NAME_IS_FILTERED.create(name).getMessage()));
                                    return;
                                }
                                renameClan(executor, executorClan, name);
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                        //dont let administrating senders use this argument, they use the other argument, which does not check for filter or correct clan name
                        .requires((source) -> !this.executorHasAClan(source) && !this.senderIsAdministrating(source))
                )
                .then(Commands.argument("Admin Clan Name", StringArgumentType.string())
                        .executes(context -> {
                            final String name = context.getArgument("Admin Clan Name", String.class);
                            final Player executor = getPlayerFromExecutor(context);
                            final Clan executorClan = getClanByExecutor(context);
                            if (clanManager.getClanByName(name).isPresent()) {
                                throw ClanArgumentException.NAME_ALREADY_EXISTS.create(name);
                            }
                            renameClan(executor, executorClan, name);
                            return Command.SINGLE_SUCCESS;
                        })
                        .requires(source -> !this.executorHasAClan(source) && this.senderIsAdministrating(source))
                );
    }

    private void renameClan(final Player player, final Clan clan, final String name){
        final String oldName = clan.getName();
        clan.setName(name);
        clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> has renamed a clan from <yellow>%s<gray> to <yellow>%s<gray>!",
                player.getName(), oldName, name), Rank.ADMIN);
        clanManager.updateClanName(clan);

        log.info("{} has renamed a clan from {} to {}!", player.getName(), oldName, name)
                .setAction("CLAN_RENAME").addClientContext(player, false).addClanContext(clan).submit();
    }

}
