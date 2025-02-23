package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestNeutralEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.ClanArgument;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierNeutralCommand extends BrigadierClanSubCommand {
    @Inject
    protected BrigadierNeutralCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "neutral";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Neutrals the specified Clan";
    }

    @Override
    protected ClanMember.MemberRank requiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
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
                .then(Commands.argument("Ally Or Enemy Clan", BPvPClansArgumentTypes.allyOrEnemyClan())
                        .executes(context -> {
                            Clan target = context.getArgument("Ally Or Enemy Clan", Clan.class);
                            if (!(context.getSource().getExecutor() instanceof Player player)) return Command.SINGLE_SUCCESS;

                            Clan origin = clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgument.NOT_IN_A_CLAN_EXCEPTION.create(player.getName()));

                            doNeutral(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        .requires(this::executorHasAClan)
                );
    }

    private void doNeutral(Player originPlayer, Clan origin, Clan target) throws CommandSyntaxException {
        //if (!target.isAllied(origin) || target.isEnemy(origin)) throw ClanArgument.CLAN_NOT_ALLY_OR_ENEMY_OF_CLAN.create(origin, target);

        UtilServer.callEvent(new ClanRequestNeutralEvent(originPlayer, origin, target));
    }


}
