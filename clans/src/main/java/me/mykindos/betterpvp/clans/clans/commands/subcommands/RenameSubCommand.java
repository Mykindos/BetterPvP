package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@CustomLog
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
        return "clans.command.rename.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.rename.no-args");
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByClient(client);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.rename.no-clan");
            return;
        }

        String clanName = args[0];
        if (!client.isAdministrating() && clanName.matches("^.*[^a-zA-Z0-9].*$")) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.rename.invalid-chars");
            return;
        }

        clanName = clanName.replace("_", " ");

        Optional<Clan> newClanOptional = clanManager.getClanByName(clanName.toLowerCase());
        if (newClanOptional.isEmpty()) {

            Clan clan = clanOptional.get();
            String oldName = clan.getName();
            clan.setName(clanName);

            Component notification = Translations.component("clans.command.clan.rename.notification",
                    Component.text(client.getName(), NamedTextColor.YELLOW),
                    Component.text(oldName, NamedTextColor.YELLOW),
                    Component.text(clanName, NamedTextColor.YELLOW));

            clientManager.getPlayersOfRank(Rank.ADMIN).forEach(target -> {
                UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
            });
            clanManager.updateClanName(clan);

            log.info("{} has renamed a clan from {} to {}!", client.getName(), oldName, clanName)
                    .setAction("CLAN_RENAME").addClientContext(client, false).addClanContext(clan).submit();
        } else {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.rename.already-exists");
        }

    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
