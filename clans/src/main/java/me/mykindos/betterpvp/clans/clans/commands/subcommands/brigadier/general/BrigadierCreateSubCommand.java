package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

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
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.chat.IFilterService;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.UUID;

@Singleton
@CustomLog
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierCreateSubCommand extends ClanBrigadierCommand {
    private final IFilterService filterService;
    @Inject
    protected BrigadierCreateSubCommand(ClientManager clientManager, ClanManager clanManager, IFilterService filterService) {
        super(clientManager, clanManager);
        this.filterService = filterService;
    }

    @Override
    public String getName() {
        return "create";
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
                .then(Commands.argument("Clan Name", BPvPClansArgumentTypes.clanName())
                        .executes(context -> {
                            final String name = context.getArgument("Clan Name", String.class);
                            if (context.getSource().getExecutor() instanceof final Player player) {
                                filterService.isFiltered(name).thenAccept((filtered) -> {
                                    if (filtered) {
                                        context.getSource().getSender()
                                                .sendMessage(UtilMessage.deserialize("<red>" +
                                                        ClanArgumentException.NAME_IS_FILTERED.create(name).getMessage()));
                                        return;
                                    }
                                    createClan(player, name);
                                });
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        //dont let administrating senders use this argument, they use the other argument, which does not check for filter or correct clan name
                        .requires((source) -> !this.executorHasAClan(source) && !this.senderIsAdministrating(source))
                )
                .then(Commands.argument("Admin Clan Name", StringArgumentType.greedyString())
                        .executes(context -> {
                            String name = context.getArgument("Admin Clan Name", String.class);
                            if (!(context.getSource().getSender() instanceof final Player player)) return Command.SINGLE_SUCCESS;

                            createClan(player, name);
                            return Command.SINGLE_SUCCESS;
                        })
                        .requires(source -> !this.executorHasAClan(source) && this.senderIsAdministrating(source))
                );
    }


    @Override
    public String getDescription() {
        return "Create the specified Clan";
    }

    private void createClan(Player creator, String name) {
        Clan clan = new Clan(UUID.randomUUID());
        clan.setName(name);
        clan.setOnline(true);
        clan.getProperties().registerListener(clan);
        UtilServer.callEvent(new ClanCreateEvent(creator, clan));
    }
}
