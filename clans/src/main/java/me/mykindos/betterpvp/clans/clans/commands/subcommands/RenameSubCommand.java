package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@Singleton
@SubCommand(ClanCommand.class)
public class RenameSubCommand extends ClanSubCommand {

    @Inject
    public RenameSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "rename";
    }

    public String getUsage() {
        return super.getUsage() + " <new name>";
    }

    @Override
    public String getDescription() {
        return "Rename a clan to a specific name";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a new name!");
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByClient(client);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You are not in a clan!");
            return;
        }

        String clanName = args[0];
        if (clanName.matches("^.*[^a-zA-Z0-9].*$")) {
            UtilMessage.message(player, "Command", "Invalid characters in Clan name.");
            return;
        }

        Optional<Clan> newClanOptional = clanManager.getObject(clanName.toLowerCase());
        if (newClanOptional.isEmpty()) {

            Clan clan = clanOptional.get();
            String oldName = clan.getName();
            clan.setName(clanName);
            clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> has renamed a clan from <yellow>%s<gray> to <yellow>%s<gray>!",
                    client.getName(), oldName, clanName), Rank.ADMIN);
            clanManager.getRepository().updateClanName(clan);
        } else {
            UtilMessage.message(player, "Command", "A clan with that name already exists.");
        }

    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
