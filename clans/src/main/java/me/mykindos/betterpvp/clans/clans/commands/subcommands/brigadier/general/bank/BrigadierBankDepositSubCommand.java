package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general.bank;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.entity.Player;

@Singleton
@CustomLog
@BrigadierSubCommand(BrigadierBankSubCommand.class)
public class BrigadierBankDepositSubCommand extends BrigadierClanSubCommand {

    @Inject
    protected BrigadierBankDepositSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    @Override
    protected ClanMember.MemberRank requiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "deposit";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Deposit funds into the clan bank";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public BPvPLiteralArgumentBuilder define() {
        return IBrigadierCommand.literal(getName())
                .then(IBrigadierCommand.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            final Player executor = getPlayerFromExecutor(context);
                            final Client executorClient = getClientFromExecutor(context);
                            final Clan executorClan = getClanByExecutor(context);

                            final Gamer gamer = executorClient.getGamer();

                            int amountToDeposit = context.getArgument("amount", Integer.class);
                            if (gamer.getBalance() < amountToDeposit) {
                                amountToDeposit = gamer.getBalance();
                            }

                            executorClan.saveProperty(ClanProperty.BALANCE, executorClan.getBalance() + amountToDeposit);
                            gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - amountToDeposit);

                            executorClan.messageClan("<yellow>" + executor.getName() + " <gray>deposited <green>$" + amountToDeposit + " <gray>into the clan bank.", null, true);
                            log.info("{} deposited ${} into clan {}", executor.getName(), amountToDeposit, executorClan.getId().toString()).submit();

                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}
