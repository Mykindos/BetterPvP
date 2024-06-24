package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class VaultSubCommand extends ClanSubCommand {

    @Inject
    public VaultSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "vault";
    }

    @Override
    public String getDescription() {
        return "Open your clan's vault";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if(locationClanOptional.isEmpty() || !locationClanOptional.get().equals(clan)) {
            UtilMessage.simpleMessage(player, "Clans", "You must be in your clan's territory to access the vault.");
            return;
        }

        final ClanVault vault = clan.getVault();
        if (!vault.hasPermission(player)) {
            UtilMessage.message(player, "Clans", "You do not have permission to access the clan vault.");
            return;
        }

        if (vault.isLocked()) {
            UtilMessage.message(player, "Clans", "<red>The clan vault is currently in use by: <dark_red>%s</dark_red>.", vault.getLockedBy());
            return;
        }

        vault.show(player);
    }

    @Override
    public String getArgumentType(int arg) {
        return ClanArgumentType.CLAN.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }

}
