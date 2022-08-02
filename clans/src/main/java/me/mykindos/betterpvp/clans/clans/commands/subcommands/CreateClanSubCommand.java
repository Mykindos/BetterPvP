package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.Optional;


public class CreateClanSubCommand extends ClanSubCommand {



    @Inject
    @Config(path = "command.clan.create.maxCharactersInClanName", defaultValue = "13")
    private int maxCharactersInClanName;

    @Inject
    @Config(path = "command.clan.create.minCharactersInClanName", defaultValue = "3")
    private int minCharactersInClanName;

    public CreateClanSubCommand(ClanManager clanManager) {
        super(clanManager);
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
        if (clanName.matches("^.*[^a-zA-Z0-9].*$")) {
            UtilMessage.message(player, "Command", "Invalid characters in Clan name.");
            return;
        }

        if (clanName.length() < minCharactersInClanName) {
            UtilMessage.message(player, "Command", "Clan name too short. Minimum length is [" + minCharactersInClanName + "].");
            return;
        }

        if (clanName.length() > maxCharactersInClanName) {
            UtilMessage.message(player, "Command", "Clan name too long. Maximum length is [" + maxCharactersInClanName + "].");
            return;
        }

        Optional<Clan> clanOptional = clanManager.getObject(clanName);
        if (clanOptional.isEmpty()) {
            var timestamp = new Timestamp(System.currentTimeMillis());
            Clan clan = Clan.builder().name(clanName).level(1)
                    .timeCreated(timestamp).lastLogin(timestamp)
                    .build();
            Bukkit.getPluginManager().callEvent(new ClanCreateEvent(player, clan));
        } else {
            UtilMessage.message(player, "Command", "A clan with that name already exists.");
        }

    }

}
