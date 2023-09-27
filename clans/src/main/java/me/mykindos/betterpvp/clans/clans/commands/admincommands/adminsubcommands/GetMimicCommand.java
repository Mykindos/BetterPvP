package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.AdminCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(AdminCommand.class)
public class GetMimicCommand extends AdminCommand {

    @Inject
    public GetMimicCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "getmimic";
    }

    @Override
    public String getDescription() {
        return "Get the clan you are mimicing";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Component component = Component.empty();

        if (!client.hasRank(Rank.ADMIN)) return;

        Optional<Clan> clanOptional = clanManager.getClanById(client.getMimicClan());
        if (clanOptional.isPresent() && client.isMimicking()) {
            Clan clan = clanOptional.get();
            component = component.append(Component.text("You are mimicking ", NamedTextColor.GRAY).append(Component.text(clan.getName(), NamedTextColor.YELLOW)));
        }
        else {
            component = component.append(Component.text("You are not currently mimicking a Clan."));
        }

        UtilMessage.message(player, "Clan Admin", component);
    }
}
