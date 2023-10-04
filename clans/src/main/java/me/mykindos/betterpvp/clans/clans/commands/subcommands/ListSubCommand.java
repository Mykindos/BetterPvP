package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Named;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Singleton
@SubCommand(ClanCommand.class)
public class ListSubCommand extends ClanSubCommand {

    @Inject
    public ListSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List all clans.";
    }

    @Override public String getUsage() {
        return super.getUsage() + " [pageNumber]";
    }


    @Override
    public void execute(Player player, Client client, String... args) {;
        int numPerPage = 10;
        int pageNumber = 1;

        if (args.length >= 1) {
            try {
                pageNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                //pass
            }
        }

        List<Clan> clansList = new ArrayList<>(clanManager.getObjects().values());
        Collections.sort(clansList, Comparator.comparing(Clan::getName));

        Component component = Component.text("Clans Page: ", NamedTextColor.YELLOW);

        int count = 0;
        int start = (pageNumber - 1) * numPerPage;
        int end = start + numPerPage;
        int size = clansList.size();

        Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);


        component = component.append(Component.text(pageNumber, NamedTextColor.WHITE));

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



    private Component addClanComponent(Clan playerClan, Clan clan) {
        Component component = Component.empty().appendNewline();

        List<ClanMember> clanMembers = clan.getMembers();
        ClanRelation clanRelation = clanManager.getRelation(playerClan, clan);

        //possible logic error, unable to test with multiple people in a Clan and one offline
        int onlineMembers = (int) clanMembers.stream().filter(member -> Bukkit.getPlayer(member.getUuid()) == null).count();

        NamedTextColor color = clanRelation.getPrimary();

        component = component.append(Component.text(clan.getName(), color))
                .append(Component.text(" (", NamedTextColor.YELLOW))
                .append(Component.text(onlineMembers, NamedTextColor.WHITE))
                .append(Component.text("|", NamedTextColor.YELLOW))
                .append(Component.text(clanMembers.size(), NamedTextColor.WHITE))
                .append(Component.text(")", NamedTextColor.YELLOW));

        return component;
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
