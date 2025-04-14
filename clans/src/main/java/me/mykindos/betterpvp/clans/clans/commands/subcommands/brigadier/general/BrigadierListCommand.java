package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierListCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierListCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "list";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "List all clans";
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
                .executes(context -> {
                    Player executor = getPlayerFromExecutor(context);
                    Clan executorClan = getClanByExecutor(context);
                    doList(executor, executorClan, 1);
                    return Command.SINGLE_SUCCESS;
                })
                .then(IBrigadierCommand.argument("Page Number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            Player executor = getPlayerFromExecutor(context);
                            Clan executorClan = getClanByExecutor(context);
                            int page = context.getArgument("Page Number", Integer.class);
                            doList(executor, executorClan, page);
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private void doList(Player player, Clan playerClan, int pageNumber) throws CommandSyntaxException {
        int numPerPage = 10;

        List<Clan> clansList = new ArrayList<>(clanManager.getObjects().values());
        Collections.sort(clansList, Comparator.comparing(Clan::getOnlineMemberCount));
        Collections.reverse(clansList);

        Component component = UtilMessage.deserialize("<yellow>Clan List<gray>: ");

        int count = 0;
        int start = (pageNumber - 1) * numPerPage;
        int end = start + numPerPage;
        int size = clansList.size();
        int totalPages = size / numPerPage;
        if (size % numPerPage > 0) {
            totalPages++;
        }


        component = component.append(UtilMessage.deserialize("<white>" + pageNumber + "<gray> / <white>" + totalPages));

        if (start <= size) {
            if (end > size) end = size;
            for (Clan clan : clansList.subList(start, end)) {
                if (count == numPerPage) break;
                component = component.append(addClanComponent(playerClan, clan));
                count++;
            }
        }
        UtilMessage.message(player, "Clans", component);
    }

    private Component addClanComponent(Clan playerClan, Clan clan) throws CommandSyntaxException {
        Component component = Component.empty().appendNewline();

        List<ClanMember> clanMembers = clan.getMembers();
        ClanRelation clanRelation = clanManager.getRelation(playerClan, clan);

        //possible logic error, unable to test with multiple people in a Clan and one offline
        int onlineMembers = clan.getOnlineMemberCount();

        NamedTextColor color = clanRelation.getPrimary();

        component = component.append(Component.text(clan.getName(), color))
                .append(Component.text(" (", NamedTextColor.YELLOW))
                .append(Component.text(onlineMembers, NamedTextColor.WHITE))
                .append(Component.text("|", NamedTextColor.YELLOW))
                .append(Component.text(clanMembers.size(), NamedTextColor.WHITE))
                .append(Component.text(")", NamedTextColor.YELLOW));

        return component;
    }


}
