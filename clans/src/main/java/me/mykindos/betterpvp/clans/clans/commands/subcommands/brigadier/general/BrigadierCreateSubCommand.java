package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.UUID;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
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
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
        return IBrigadierCommand.literal(getName())
                .then(Commands.argument("Clan Name", BPvPClansArgumentTypes.clanName())
                        .executes(context -> {
                            final String name = context.getArgument("Clan Name", String.class);
                            final Player player = getPlayerFromExecutor(context);
                            filterService.isFiltered(name)
                                    .exceptionally(throwable -> {
                                        log.error("Error filtering Clan", throwable).submit();
                                        return true;
                                    })
                                    .thenAccept((filtered) -> {
                                        if (filtered) {
                                            UtilMessage.sendCommandSyntaxException(context.getSource().getSender(), ClanArgumentException.NAME_IS_FILTERED.create(name));
                                            return;
                                        }
                                        createClan(player, name);
                                    });
                            return Command.SINGLE_SUCCESS;
                        })
                        //dont let administrating senders use this argument, they use the other argument, which does not check for filter or correct clan name
                        .requires((source) -> !this.executorHasAClan(source) && !this.senderIsAdministrating(source))
                )
                .then(Commands.argument("Admin Clan Name", StringArgumentType.string())
                        .executes(context -> {
                            final String name = context.getArgument("Admin Clan Name", String.class);
                            final Player player = getPlayerFromExecutor(context);
                            if (clanManager.getClanByName(name).isPresent()) {
                                throw ClanArgumentException.NAME_ALREADY_EXISTS.create(name);
                            }
                            createClan(player, name);
                            return Command.SINGLE_SUCCESS;
                        })
                        .requires(source -> !this.executorHasAClan(source) && this.senderIsAdministrating(source))
                );
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return super.requirement(source) && !executorHasAClan(source);
    }

    @Override
    public Component getRequirementComponent(CommandContext<CommandSourceStack> context) {
        Component component = super.getRequirementComponent(context);
        boolean inClan = executorHasAClan(context.getSource());
        component = component.appendNewline();
        component = component.append(Component.text("Need a Clan: ", NamedTextColor.WHITE))
                .append(Component.text(false, NamedTextColor.RED).append(Component.text(" | ", NamedTextColor.GRAY))
                        .append((Component.text("You: ", NamedTextColor.WHITE))
                                .append(Component.text(inClan, inClan ? NamedTextColor.GREEN : NamedTextColor.RED))));
        return component;
    }


    @Override
    public String getDescription() {
        return "Creates the specified Clan";
    }

    private void createClan(Player creator, String name) {
        UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
            Clan clan = new Clan(UUID.randomUUID());
            clan.setName(name);
            clan.setOnline(true);
            clan.getProperties().registerListener(clan);
            ClanCreateEvent event = new ClanCreateEvent(creator, clan);
            UtilServer.callEvent(event);
        });
    }
}
