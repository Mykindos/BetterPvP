package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@Singleton
@SubCommand(ClanCommand.class)
public class CreateClanSubCommand extends ClanSubCommand {

    @Inject
    @Config(path = "command.clan.create.maxCharactersInClanName", defaultValue = "13")
    private int maxCharactersInClanName;

    @Inject
    @Config(path = "command.clan.create.minCharactersInClanName", defaultValue = "3")
    private int minCharactersInClanName;

    @Inject
    public CreateClanSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <name>";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Command", "You did not input a clan name");
            return;
        }

        if (clanManager.getClanByClient(client).isPresent()) {
            UtilMessage.message(player, "Command", "You are already in a clan");
            return;
        }

        String clanName = args[0];

        if (clanName.length() < minCharactersInClanName) {
            UtilMessage.message(player, "Command", "Clan name too short. Minimum length is [" + minCharactersInClanName + "].");
            return;
        }

        if (clanName.length() > maxCharactersInClanName) {
            UtilMessage.message(player, "Command", "Clan name too long. Maximum length is [" + maxCharactersInClanName + "].");
            return;
        }

        if (clanName.matches("^.*[^a-zA-Z0-9].*$")) {
            UtilMessage.message(player, "Command", "Invalid characters in Clan name.");
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByName(clanName.toLowerCase());
        if (clanOptional.isEmpty()) {
            Clan clan = new Clan(UUID.randomUUID());
            clan.setName(clanName);
            clan.setOnline(true);
            clan.setAdmin(client.isAdministrating());
            clan.getProperties().registerListener(clan);

            UtilServer.callEvent(new ClanCreateEvent(player, clan));
        } else {
            UtilMessage.message(player, "Command", "A clan with that name already exists.");
        }

    }

    @Override
    public boolean canExecuteWithoutClan(){
        return true;
    }

}
