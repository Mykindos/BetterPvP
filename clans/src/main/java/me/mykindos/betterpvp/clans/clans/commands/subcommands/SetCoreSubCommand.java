package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanSetCoreEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class SetCoreSubCommand extends ClanSubCommand {

    @Inject
    public SetCoreSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
        this.aliases.add("sethome");
    }

    @Override
    public String getName() {
        return "setcore";
    }

    @Override
    public String getDescription() {
        return "Set the clan core location for your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        if (!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "You must be a clan admin or above to use this command");
            return;
        }

        Location bedLocation = player.getLocation();
        Block block = bedLocation.getBlock();
        if (!block.getType().isAir() || !block.getRelative(player.getFacing()).getType().isAir()) {
            UtilMessage.message(player, "Clans", "You must have a clear space to set your core.");
            return;
        }

        UtilServer.callEvent(new ClanSetCoreEvent(player, playerClan));
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
