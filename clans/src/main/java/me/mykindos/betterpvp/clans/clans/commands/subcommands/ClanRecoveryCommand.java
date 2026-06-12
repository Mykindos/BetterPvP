package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

@Singleton
@SubCommand(ClanCommand.class)
public class ClanRecoveryCommand extends ClanSubCommand {

    @Inject
    public ClanRecoveryCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "clanrecovery";
    }

    @Override
    public String getDescription() {
        return "clans.command.clan-recovery.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        Component notification = Translations.component("clans.command.clan.recovery.notification",
                Component.text(player.getName(), NamedTextColor.YELLOW),
                Component.text(clan.getName(), NamedTextColor.YELLOW));

        clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
            UtilMessage.message(target, CLANS_PREFIX, notification);
        });

        clanManager.startInsuranceRollback(clan);

    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
