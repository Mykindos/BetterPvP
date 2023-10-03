package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@SubCommand(ClanCommand.class)
public class ListSubCommand extends ClanSubCommand {

    @Inject
    public ListSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        aliases.addAll(List.of("?", "h"));

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

        List<Clan> clansList = clanManager.getRepository().getAll();

        Collections.sort(clansList, Comparator.comparing(Clan::getName));

        Component component = Component.text("Clans Page: ", NamedTextColor.YELLOW);

        int count = 0;
        int start = (pageNumber - 1) * numPerPage;
        int end = start + numPerPage;
        int size = clansList.size();

        component = component.append(Component.text(pageNumber, NamedTextColor.WHITE));

        if (start <= size) {
            if (end > size) end = size;
            for (Clan clan : clansList.subList(start, end)) {
                if (count == numPerPage) break;
                component = component.append(addClanComponent(clan));
                count++;
            }
        }
        UtilMessage.message(player, "Clans", component);
    }



    private Component addClanComponent(Clan clan) {
        Component component = Component.empty().appendNewline();

        List<ClanMember> clanMembers = clan.getMembers();

        int onlineMembers = (int) clanMembers.stream().filter(member -> Bukkit.getPlayer(member.getUuid()) != null).count();

        NamedTextColor color = onlineMembers > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;

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
